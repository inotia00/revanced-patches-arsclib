package app.revanced.patches.youtube.video.information

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patcher.util.smali.toInstructions
import app.revanced.patches.youtube.utils.fingerprints.OrganicPlaybackContextModelFingerprint
import app.revanced.patches.youtube.utils.fingerprints.VideoEndFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.VIDEO_PATH
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.video.information.fingerprints.OnPlaybackSpeedItemClickFingerprint
import app.revanced.patches.youtube.video.information.fingerprints.PlaybackSpeedClassFingerprint
import app.revanced.patches.youtube.video.information.fingerprints.PlayerControllerSetTimeReferenceFingerprint
import app.revanced.patches.youtube.video.information.fingerprints.VideoInformationPatchFingerprint
import app.revanced.patches.youtube.video.information.fingerprints.VideoLengthFingerprint
import app.revanced.patches.youtube.video.playerresponse.PlayerResponseMethodHookPatch
import app.revanced.patches.youtube.video.videoid.VideoIdPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getWalkerMethod
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.MethodUtil

@Patch(
    description = "Hooks YouTube to get information about the current playing video.",
    dependencies = [
        PlayerResponseMethodHookPatch::class,
        PlayerTypeHookPatch::class,
        VideoIdPatch::class
    ]
)
object VideoInformationPatch : BytecodePatch(
    setOf(
        OnPlaybackSpeedItemClickFingerprint,
        OrganicPlaybackContextModelFingerprint,
        PlaybackSpeedClassFingerprint,
        PlayerControllerSetTimeReferenceFingerprint,
        VideoEndFingerprint,
        VideoInformationPatchFingerprint,
        VideoLengthFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$VIDEO_PATH/VideoInformation;"

    private lateinit var playerConstructorMethod: MutableMethod
    private var playerConstructorInsertIndex = 4

    private lateinit var videoTimeConstructorMethod: MutableMethod
    private var videoTimeConstructorInsertIndex = 2

    private lateinit var videoCpnConstructorMethod: MutableMethod
    private var videoCpnConstructorInsertIndex = 2

    // Used by other patches.
    internal lateinit var speedSelectionInsertMethod: MutableMethod

    override fun execute(context: BytecodeContext) {
        VideoEndFingerprint.resultOrThrow().let {
            playerConstructorMethod =
                it.mutableClass.methods.first { method -> MethodUtil.isConstructor(method) }

            // hook the player controller for use through integrations
            onCreateHook(INTEGRATIONS_CLASS_DESCRIPTOR, "initialize")

            it.mutableMethod.apply {
                val seekSourceEnumType = parameterTypes[1].toString()

                it.mutableClass.methods.add(
                    ImmutableMethod(
                        definingClass,
                        "seekTo",
                        listOf(ImmutableMethodParameter("J", annotations, "time")),
                        "Z",
                        AccessFlags.PUBLIC or AccessFlags.FINAL,
                        annotations,
                        null,
                        ImmutableMethodImplementation(
                            4, """
                                sget-object v0, $seekSourceEnumType->a:$seekSourceEnumType
                                invoke-virtual {p0, p1, p2, v0}, ${definingClass}->${name}(J$seekSourceEnumType)Z
                                move-result p1
                                return p1
                                """.toInstructions(),
                            null,
                            null
                        )
                    ).toMutable()
                )

                val videoEndMethod = getWalkerMethod(context, it.scanResult.patternScanResult!!.startIndex + 1)

                videoEndMethod.apply {
                    addInstructionsWithLabels(
                        0, """
                            invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->videoEnded()Z
                            move-result v0
                            if-eqz v0, :end
                            return-void
                            """, ExternalLabel("end", getInstruction(0))
                    )
                }
            }
        }

        /**
         * Set current video time method
         */
        PlayerControllerSetTimeReferenceFingerprint.resultOrThrow().let {
            videoTimeConstructorMethod =
                it.getWalkerMethod(context, it.scanResult.patternScanResult!!.startIndex)
        }

        /**
         * Set current video time
         */
        videoTimeHook(INTEGRATIONS_CLASS_DESCRIPTOR, "setVideoTime")

        /**
         * Set current video length
         */
        VideoLengthFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val primaryRegister = getInstruction<OneRegisterInstruction>(startIndex).registerA
                val secondaryRegister = primaryRegister + 1

                addInstruction(
                    startIndex + 2,
                    "invoke-static {v$primaryRegister, v$secondaryRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->setVideoLength(J)V"
                )
            }
        }

        /**
         * Set current video is livestream
         */
        videoCpnConstructorMethod = OrganicPlaybackContextModelFingerprint.resultOrThrow().mutableMethod
        cpnHook("$INTEGRATIONS_CLASS_DESCRIPTOR->setLiveStreamState(Ljava/lang/String;Z)V")

        /**
         * Set current video id
         */
        val videoIdMethodDescriptor = "$INTEGRATIONS_CLASS_DESCRIPTOR->setVideoId(Ljava/lang/String;)V"
        VideoIdPatch.hookVideoId(videoIdMethodDescriptor)
        VideoIdPatch.hookBackgroundPlayVideoId(videoIdMethodDescriptor)
        VideoIdPatch.hookPlayerResponseVideoId(
            "$INTEGRATIONS_CLASS_DESCRIPTOR->setPlayerResponseVideoId(Ljava/lang/String;Z)V")
        // Call before any other video id hooks,
        // so they can use VideoInformation and check if the video id is for a Short.
        PlayerResponseMethodHookPatch += PlayerResponseMethodHookPatch.Hook.PlayerParameterBeforeVideoId(
            "$INTEGRATIONS_CLASS_DESCRIPTOR->newPlayerResponseParameter(Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;")

        /**
         * Hook the user playback speed selection
         */
        OnPlaybackSpeedItemClickFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                speedSelectionInsertMethod = this
                val speedSelectionValueInstructionIndex = getTargetIndex(Opcode.IGET)

                val setPlaybackSpeedContainerClassFieldIndex = getTargetIndexReversed(speedSelectionValueInstructionIndex, Opcode.IGET_OBJECT)
                val setPlaybackSpeedContainerClassFieldReference =
                    getInstruction<ReferenceInstruction>(setPlaybackSpeedContainerClassFieldIndex).reference.toString()

                val setPlaybackSpeedClassFieldReference =
                    getInstruction<ReferenceInstruction>(speedSelectionValueInstructionIndex + 1).reference.toString()
                val setPlaybackSpeedMethodReference =
                    getInstruction<ReferenceInstruction>(speedSelectionValueInstructionIndex + 2).reference.toString()

                it.mutableClass.methods.add(
                    ImmutableMethod(
                        definingClass,
                        "overridePlaybackSpeed",
                        listOf(ImmutableMethodParameter("F", annotations, null)),
                        "V",
                        AccessFlags.PUBLIC or AccessFlags.PUBLIC,
                        annotations,
                        null,
                        ImmutableMethodImplementation(
                            4, """
                                const/4 v0, 0x0
                                cmpg-float v0, v3, v0
                                if-lez v0, :ignore
                                
                                # Get the container class field.
                                iget-object v0, v2, $setPlaybackSpeedContainerClassFieldReference  
                                                                
                                # Get the field from its class.
                                iget-object v1, v0, $setPlaybackSpeedClassFieldReference
                                
                                # Invoke setPlaybackSpeed on that class.
                                invoke-virtual {v1, v3}, $setPlaybackSpeedMethodReference

                                :ignore
                                return-void
                                """.toInstructions(), null, null
                        )
                    ).toMutable()
                )

                val walkerMethod = getWalkerMethod(context, speedSelectionValueInstructionIndex + 2)

                walkerMethod.apply {
                    addInstruction(
                        this.implementation!!.instructions.size - 1,
                        "invoke-static { p1 }, $INTEGRATIONS_CLASS_DESCRIPTOR->setPlaybackSpeed(F)V"
                    )
                }
            }
        }

        PlaybackSpeedClassFingerprint.resultOrThrow().let { result ->
            result.mutableMethod.apply {
                val index = result.scanResult.patternScanResult!!.endIndex
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                val playbackSpeedClass = this.returnType
                replaceInstruction(
                    index,
                    "sput-object v$register, $INTEGRATIONS_CLASS_DESCRIPTOR->playbackSpeedClass:$playbackSpeedClass"
                )
                addInstruction(
                    index + 1,
                    "return-object v$register"
                )

                VideoInformationPatchFingerprint.resultOrThrow().let {
                    it.mutableMethod.apply {
                        it.mutableClass.staticFields.add(
                            ImmutableField(
                                definingClass,
                                "playbackSpeedClass",
                                playbackSpeedClass,
                                AccessFlags.PUBLIC or AccessFlags.STATIC,
                                null,
                                annotations,
                                null
                            ).toMutable()
                        )

                        addInstructions(
                            0, """
                                sget-object v0, $INTEGRATIONS_CLASS_DESCRIPTOR->playbackSpeedClass:$playbackSpeedClass
                                invoke-virtual {v0, p0}, $playbackSpeedClass->overridePlaybackSpeed(F)V
                                return-void
                                """
                        )
                    }
                }
            }
        }
    }

    private fun MutableMethod.insert(insertIndex: Int, register: String, descriptor: String) =
        addInstruction(insertIndex, "invoke-static { $register }, $descriptor")

    private fun MutableMethod.insertTimeHook(insertIndex: Int, descriptor: String) =
        insert(insertIndex, "p1, p2", descriptor)

    /**
     * Hook the player controller.  Called when a video is opened or the current video is changed.
     *
     * Note: This hook is called very early and is called before the video id, video time, video length,
     * and many other data fields are set.
     *
     * @param targetMethodClass The descriptor for the class to invoke when the player controller is created.
     * @param targetMethodName The name of the static method to invoke when the player controller is created.
     */
    internal fun onCreateHook(targetMethodClass: String, targetMethodName: String) =
        playerConstructorMethod.insert(
            playerConstructorInsertIndex++,
            "v0",
            "$targetMethodClass->$targetMethodName(Ljava/lang/Object;)V"
        )

    /**
     * Hook the video time.
     * The hook is usually called once per second.
     *
     * @param targetMethodClass The descriptor for the static method to invoke when the player controller is created.
     * @param targetMethodName The name of the static method to invoke when the player controller is created.
     */
    internal fun videoTimeHook(targetMethodClass: String, targetMethodName: String) =
        videoTimeConstructorMethod.insertTimeHook(
            videoTimeConstructorInsertIndex++,
            "$targetMethodClass->$targetMethodName(J)V"
        )

    internal fun cpnHook(descriptor: String) =
        videoCpnConstructorMethod.insert(
            videoCpnConstructorInsertIndex++,
            "p1, p2",
            descriptor
        )
}
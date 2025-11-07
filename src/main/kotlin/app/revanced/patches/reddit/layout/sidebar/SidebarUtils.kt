package app.revanced.patches.reddit.layout.sidebar

import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.reference.FieldReference

internal object SidebarUtils {
    internal fun indexOfHeaderItemInstruction(
        methodDef: Method,
        fieldName: String = "RECENTLY_VISITED",
    ) = methodDef.indexOfFirstInstruction {
        getReference<FieldReference>()?.name == fieldName
    }
}

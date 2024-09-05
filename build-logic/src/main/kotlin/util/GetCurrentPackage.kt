package util

import org.gradle.api.Project

fun Project.getCurrentPackage(): String {
    val namespaceParts: MutableList<String> = mutableListOf()
    var currentProject: Project? = this
    while (currentProject != null) {
        namespaceParts.add(currentProject.name)
        currentProject = currentProject.parent
    }

    return "dev.toastbits." + namespaceParts.asReversed().joinToString(".")
}

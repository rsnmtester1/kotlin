/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.noarg.diagnostic

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny

class CliNoArgDeclarationChecker(private val noArgAnnotationFqNames: List<String>) : AbstractNoArgDeclarationChecker() {
    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?) = noArgAnnotationFqNames
}

abstract class AbstractNoArgDeclarationChecker : DeclarationChecker, AnnotationBasedExtension {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor || declaration !is KtClass) return
        if (descriptor.kind != ClassKind.CLASS) return
        if (!descriptor.hasSpecialAnnotation(declaration)) return

        if (descriptor.isInner) {
            context.trace.report(ErrorsNoArg.NOARG_ON_INNER_CLASS.on(declaration.reportTarget))
        } else if (DescriptorUtils.isLocal(descriptor)) {
            context.trace.report(ErrorsNoArg.NOARG_ON_LOCAL_CLASS.on(declaration.reportTarget))
        }

        val superClass = descriptor.getSuperClassOrAny()
        if (superClass.constructors.none { it.isNoArgConstructor() } && !superClass.hasSpecialAnnotation(declaration)) {
            context.trace.report(ErrorsNoArg.NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS.on(declaration.reportTarget))
        }
    }

    private val KtClass.reportTarget: PsiElement
        get() = nameIdentifier ?: getClassOrInterfaceKeyword() ?: this

    private fun ConstructorDescriptor.isNoArgConstructor(): Boolean =
        valueParameters.all(ValueParameterDescriptor::declaresDefaultValue)
}

package org.intellij.sequencer;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.intellij.sequencer.generator.SequenceParams;
import org.intellij.sequencer.util.MyPsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.KtFunction;

import java.util.Arrays;
import java.util.Collection;

import static org.intellij.sequencer.util.ConfigUtil.loadSequenceParams;

/**
 * Show Sequence generate options dialog.
 */
public class ShowSequenceAction extends AnAction {

    public ShowSequenceAction() {
    }

    /**
     * Enable or disable the menu base on file type. Current only java file will enable the menu.
     *
     * @param event event
     */
    public void update(@NotNull AnActionEvent event) {
        super.update(event);

        Presentation presentation = event.getPresentation();

        PsiElement psiElement = event.getData(CommonDataKeys.PSI_FILE);
        presentation.setEnabled(isEnabled(psiElement));

    }

    private boolean isEnabled(PsiElement psiElement) {
        // only JAVA or Kotlin will enable the generator.
        return psiElement != null
                && (psiElement.getLanguage().is(JavaLanguage.INSTANCE)
                || psiElement.getLanguage().is(KotlinLanguage.INSTANCE)
        );
    }

    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;

        SequenceService plugin = project.getService(SequenceService.class);

        SequenceParams params = loadSequenceParams();

        PsiElement psiElement = event.getData(CommonDataKeys.PSI_ELEMENT);
        if (psiElement == null) {
            // try to find the enclosedMethod of caret (java)
            final PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
            final Caret caret = event.getData(CommonDataKeys.CARET);
            if (psiFile != null && caret != null && psiFile.getLanguage() == JavaLanguage.INSTANCE) {
                psiElement = MyPsiUtil.getEnclosingMethod(psiFile, caret.getOffset());
            }

            // try to get top PsiClass (java)
            if (psiElement == null && psiFile != null && psiFile.getLanguage() == JavaLanguage.INSTANCE) {
                final Collection<PsiClass> psiClassCollection = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass.class);
                for (PsiClass psiClass : psiClassCollection) {
                    chooseMethodToGenerate(event, plugin, params, psiClass);
                }
            }
        }

        if (psiElement instanceof PsiClass) {
            chooseMethodToGenerate(event, plugin, params, (PsiClass) psiElement);
        } else if (psiElement instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) psiElement;
            plugin.showSequence(params, method);
        } else if (psiElement instanceof KtFunction) {
            // generate kotlin function
            plugin.showSequence(params, psiElement);
        }

    }

    private void chooseMethodToGenerate(@NotNull AnActionEvent event, SequenceService plugin, SequenceParams params, PsiClass psiElement) {
        PsiMethod[] methods = psiElement.getMethods();
        // for PsiClass, show popup menu list method to choose
        JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<PsiMethod>("Choose Method ...", Arrays.asList(methods)) {
            @Override
            public @NotNull
            String getTextFor(PsiMethod value) {
                return value.getName();
            }

            @Override
            public @Nullable
            PopupStep<?> onChosen(PsiMethod selectedValue, boolean finalChoice) {
                return doFinalStep(() -> plugin.showSequence(params, selectedValue));
            }
        }).showInBestPositionFor(event.getDataContext());
    }
}

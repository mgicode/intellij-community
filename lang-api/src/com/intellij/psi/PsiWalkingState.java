package com.intellij.psi;

import com.intellij.openapi.diagnostic.Logger;

/**
 * @author cdr
 */
public abstract class PsiWalkingState {
  private static final Logger LOG = Logger.getInstance("#com.intellij.psi.PsiWalkingState");
  private boolean isDown;
  private boolean startedWalking;
  private final PsiElementVisitor myVisitor;
  private boolean stopped;

  public abstract void elementFinished(PsiElement element);

  protected PsiWalkingState(PsiElementVisitor delegate) {
    myVisitor = delegate;
  }

  public void elementStarted(PsiElement element){
    isDown = true;
    if (!startedWalking) {
      if (element instanceof PsiCompiledElement) {
        // do not walk inside compiled PSI since getNextSibling() is too slow there
        LOG.error(element+"; Do not use walking visitor inside compiled PSI since getNextSibling() is too slow there");
      }
      stopped = false;
      startedWalking = true;
      walkChildren(element);
      startedWalking = false;
    }
  }

  private void walkChildren(PsiElement root) {
    for (PsiElement element = next(root,root,isDown); element != null && !stopped; element = next(element, root, isDown)) {
      isDown = false; // if client visitor did not call default visitElement it means skip subtree
      PsiElement parent = element.getParent();
      PsiElement next = element.getNextSibling();
      element.accept(myVisitor);
      assert element.getNextSibling() == next;
      assert element.getParent() == parent;
    }
  }

  private PsiElement next(PsiElement element, PsiElement root, boolean isDown) {
    if (isDown) {
      PsiElement child = element.getFirstChild();
      if (child != null) return child;
    }
    // up
    while (element != root) {
      PsiElement next = element.getNextSibling();

      elementFinished(element);
      if (next != null) {
        assert next.getPrevSibling() == element : "Element: "+element+"; next.prev: "+next.getPrevSibling()+"; File: "+element.getContainingFile();
        return next;
      }
      element = element.getParent();
    }
    elementFinished(element);
    return null;
  }

  public void startedWalking() {
    startedWalking = true;
  }

  public void stopWalking() {
    stopped = true;
  }
}

package de.lessvoid.nifty.controls.dropdown.controller;

import java.util.Properties;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Controller;
import de.lessvoid.nifty.elements.ControllerEventListener;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.xml.xpp3.Attributes;

public class DropDownPopup implements Controller {
  private Nifty nifty;
  private Screen screen;
  private Element dropDownElement;

  public void bind(
      final Nifty niftyParam,
      final Screen screenParam,
      final Element element,
      final Properties parameter,
      final ControllerEventListener listener,
      final Attributes controlDefinitionAttributes) {
    nifty = niftyParam;
    screen = screenParam;
  }

  public void inputEvent(final NiftyInputEvent inputEvent) {
  }

  public void onFocus(final boolean getFocus) {
  }

  public void onStartScreen() {
  }

  public void setDropDownElement(final Element element) {
    dropDownElement = element;
  }

  public void close() {
    dropDownElement.getControl(DropDownControl.class).reset();
    nifty.closePopup("dropDownBoxSelectPopup");
  }
}

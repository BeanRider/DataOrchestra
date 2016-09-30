package main.ui.UIAction;

import main.Main;
import processing.event.MouseEvent;

/**
 * Created by jeffrey02px2014 on 5/15/16.
 */
public abstract class Action {

  public static final Action DEFAULT = new Action() {
    @Override
    public void act(Main controller, MouseEvent e) {
      // DO NOTHING
    }
  };

  public abstract void act(Main controller, MouseEvent e);
}
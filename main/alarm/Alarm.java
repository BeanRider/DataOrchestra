package main.alarm;

import main.Main;
import main.ui.UIAction.Action;
import processing.event.MouseEvent;

/**
 * Created by jeffrey02px2014 on 6/19/16.
 */
public interface Alarm {

  void tick(Main c, MouseEvent e);

  void restartFromBeginning();

  void startCountDown();

  void pauseCountDown();

  boolean queryCompletionStatus();

  void addCompletionAction(Action a);
}

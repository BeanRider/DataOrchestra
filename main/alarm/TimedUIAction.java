package main.alarm;

import main.Main;
import main.ui.UIAction.Action;
import processing.event.MouseEvent;

import java.util.Optional;

public class TimedUIAction implements Alarm {

  private int totalTime = 0;
  private int currentTime = 0;
  private boolean timing = false;
  private Optional<Action> completedAction = Optional.empty();

  public TimedUIAction(int length) {
    if (length <= 0) {
      throw new IllegalArgumentException("A timer must have a length of bigger than 0!");
    }
    totalTime = length;
    currentTime = totalTime;
  }

  @Override
  public void tick(Main c, MouseEvent e) {
    if (!timing) {
      return;
    }

    if (currentTime > 0) {
      currentTime--;
    } else {
      currentTime = 0;
      if (completedAction.isPresent()) {
        completedAction.get().act(c, e);
      }
    }
  }

  @Override
  public void restartFromBeginning() {
    timing = false;
    currentTime = totalTime;
  }

  @Override
  public void startCountDown() {
    timing = true;
  }

  @Override
  public void pauseCountDown() {
    timing = false;
  }

  @Override
  public boolean queryCompletionStatus() {
    return currentTime == 0;
  }

  @Override
  public void addCompletionAction(Action a) {
    this.completedAction = Optional.of(a);
  }
}

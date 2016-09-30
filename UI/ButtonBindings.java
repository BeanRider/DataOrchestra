package UI;

import java.util.HashMap;

import main.Main;

public class ButtonBindings {

	HashMap<Button, VisualButtonEvent> buttonBindings = new HashMap<>();
	Main main;
	
	public ButtonBindings(Main p) {
		main = p;
	}
	
	public void bind(Button b, VisualButtonEvent e) {
		buttonBindings.put(b, e);
	}
	
	public VisualButtonEvent get(Button b) {
		return buttonBindings.get(b);
	}
	
}

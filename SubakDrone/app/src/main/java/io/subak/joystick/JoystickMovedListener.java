package io.subak.joystick;

/**
 * Created by jeyong on 5/26/15.
 */
public interface JoystickMovedListener {
    public void OnMoved(int pan, int tilt);
    public void OnReleased();
    public void OnReturnedToCenter();
}


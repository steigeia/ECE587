package nars.lab.narclear.jbox2d;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;

public interface WorldCreator {
  World createWorld(Vec2 gravity);
}

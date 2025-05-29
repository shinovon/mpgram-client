

import org.bouncycastle.math.ec.ECPointMap;

public interface ECEndomorphism
{
    ECPointMap getPointMap();

    boolean hasEfficientPointMap();
}

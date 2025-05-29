

import java.math.BigInteger;

public interface GLVEndomorphism extends ECEndomorphism
{
    BigInteger[] decomposeScalar(BigInteger k);
}

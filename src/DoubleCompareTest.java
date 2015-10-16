import static org.junit.Assert.*;

import org.junit.Test;


public class DoubleCompareTest {

	@Test
	public void equalTest() {
		assertTrue(DoubleCompare.equals(3.01, 3.0100000001));
		assertFalse(DoubleCompare.equals(3.01, 3.04));
	}
	@Test
	public void lessThanTest() {
		assertFalse(DoubleCompare.lessThan(3.01, 3.0100000001));
		assertTrue(DoubleCompare.lessThan(3.01, 3.04));
		assertFalse(DoubleCompare.lessThan(3.04, 3.03));
	}
	@Test
	public void greaterThanTest() {
		assertFalse(DoubleCompare.greaterThan(3.01, 3.0100000001));
		assertFalse(DoubleCompare.greaterThan(3.01, 3.04));
		assertTrue(DoubleCompare.greaterThan(3.04, 3.03));
	}
	
}

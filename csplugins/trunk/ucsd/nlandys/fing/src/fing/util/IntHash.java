package fing.util;

/**
 * An insert-only hashtable that has non-negative 32 bit integers as keys;
 * no values are stored in this hashtable.  An instance of this class is good
 * at detecting collisions between integers.<p>
 * In the underlying implementation, this hashtable increases in size to adapt
 * to elements being added (the underlying size of the hashtable is invisible
 * to the programmer).  In the underlying implementation, this hashtable never
 * decreases in size.  As a hashtable increases in size,
 * it takes at most 3 times as much memory as it would take
 * to store the hashtable's elements in a perfectly-sized array.
 * Underlying size expansions are implemented such that the operation of
 * expanding in size is amortized over the contstant time complexity needed to
 * insert new elements.
 */
public final class IntHash
{

  private static final int INITIAL_SIZE = 11;
  private static final double THRESHOLD_FACTOR = 0.666;

  private int[] m_arr;
  private int m_elements;
  private int m_size;
  private int m_thresholdSize;

  /**
   * Create a new hashtable.
   */
  public IntHash()
  {
    m_arr = new int[INITIAL_SIZE];
    empty();
  }

  /**
   * Returns -1 if this value is already in the hashtable, otherwise
   * returns the value that has been inserted into the hashtable.<p>
   * Only non-negative values can be passed to this method.
   * Behavior is undefined If negative values are passed to put(int).<p>
   * Insertions into the hashtable are performed in [amortized] constant time.
   */
  public final int put(int val)
  {
  }

  /**
   * Removes all elements from this hashtable.  This is a constant time
   * operation.
   */
  public final void empty()
  {
    m_elements = 0;
    m_size = INITIAL_SIZE;
    m_thresholdSize = (int) (THRESHOLD_FACTOR * (double) m_size);
    for (int i = 0; i < m_size; i++) m_arr[i] = -1;
  }

  /**
   * Returns an enumeration of elements in this hashtable, ordered
   * arbitrarily.<p>
   * The returned enumeration becomes "invalid" as soon as any other method
   * on this hashtable instance is called; calling methods on an invalid
   * enumeration will cause undefined behavior in the enumerator.<p>
   * The returned enumerator has absolutely no effect on the underlying
   * hashtable in all cases.<p>
   * This method returns a value in constant time.  The returned enumerator
   * returns successive elements in [amortized] constant time.
   */
  public final IntEnumerator elements()
  {
    final int[] array = m_arr;
    final int numElements = m_elements;
    return new IntEnumerator() {
        int elements = numElements;
        int index = 0;
        public int numRemaining() { return elements; }
        public int nextInt() {
          int returnVal;
          for (returnVal = array[index++]; returnVal < 0; index++)
            returnVal = array[index];
          elements--;
          return returnVal; } };
  }

  private final void checkSize()
  {
    if (m_elements >= m_thresholdSize) {
      final int newSize = (int)
        Math.min((long) Integer.MAX_VALUE,
                 ((long) m_size) * 2l + 1l);
      if (newSize <= m_size)
        throw new IllegalStateException
          ("too many elements in this hashtable");
      if (m_arr.length < newSize) {
        final int[] newArr = new int[newSize];
        System.arraycopy(m_arr, 0, newArr, 0, m_size);
        m_arr = newArr; }
      
  }

}

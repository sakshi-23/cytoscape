package fing.util.test;

import fing.util.IntIterator;
import fing.util.MinIntHeap;

import java.io.IOException;
import java.io.InputStream;

public class MinIntHeapPerformance
{

  /**
   * Argument at index 0: a number representing the number of elements to be
   * tossed onto a heap.  If N elements are tossed onto a heap, each element
   * shall be in the range [0, N-1].<p>
   * Standard input is read, and should contain random bytes of input, with
   * enough
   * bytes to define N integers (each integer is 4 bytes).  Integers are
   * defined from the input by taking groups of 4 consecutive bytes from input,
   * each group defining a single integer by interpreting the first byte in
   * a group to be the most significant bits of the integer etc.  The
   * range [0, N-1] of each integer is satisifed by dividing each assembled
   * four-byte integer by N, and taking the remainder as the element to be
   * tossed onto the heap.<p>
   * Output to standard out is the ordered set of input integers with
   * duplicates pruned, such that each output integer is followed by the
   * system's newline separator character sequence.<p>
   * Output to standard error is the time taken to use the heap to order
   * the input, with duplicates removed.  The output format is simply an
   * integer representing the number of milliseconds required for this
   * test case.  Basically, a timer starts
   * right before calling the MinIntHeap constructor with an array of
   * input integers; the timer stops after we've instantiated a new array to
   * contain the ordered list of elements with duplicates removed, and after
   * we've completely filled the array with these elements.  Note that
   * the process of instantiating this array is time consuming and has nothing
   * to do with the algorithm we're trying to test; this operation is
   * included in this time trial anyways because the size of the array is
   * a function of this algorithm - well, whatever - I guess I could
   * precompute this size or else run this algorithm twice.  I'd rather not.
   */
  public static void main(String[] args) throws Exception
  {
    int N = Integer.parseInt(args[0]);
    int[] elements = new int[N];
    InputStream in = System.in;
    byte[] buff = new byte[4];
    int inx = 0;
    int off = 0;
    int read;
    while (inx < N && (read = in.read(buff, off, buff.length - off)) > 0) {
      off += read;
      if (off < buff.length) continue;
      else off = 0;
      elements[inx++] = assembleInt(buff) % N; }
    if (inx < N) throw new IOException("premature end of input");
    // Lose reference to as much as we can.
    in = null;
    buff = null;
    // Sleep, collect garbage, have a snack, etc.
    // Start timer.
    int[] orderedElements = _THE_TEST_CASE_(elements);
    // Stop timer.
    // Print the time taken to standard out.
    // Print sorted array to standard out.
    for (int i = 0; i < orderedElements.length; i++)
      System.out.println(orderedElements[i]);
  }

  private static final int assembleInt(byte[] fourConsecutiveBytes)
  {
    int firstByte = (((int) fourConsecutiveBytes[0]) & 0x000000ff) << 24;
    int secondByte = (((int) fourConsecutiveBytes[1]) & 0x000000ff) << 16;
    int thirdByte = (((int) fourConsecutiveBytes[2]) & 0x000000ff) << 8;
    int fourthByte = (((int) fourConsecutiveBytes[3]) & 0x000000ff) << 0;
    return firstByte | secondByte | thirdByte | fourthByte;
  }

  // Keep a reference to our data structure so that we can determine how
  // much memory was consumed by our algorithm.
  static MinIntHeap _THE_HEAP_ = null;

  private static final int[] _THE_TEST_CASE_(int[] elements)
  {
    _THE_HEAP_ = new MinIntHeap(elements, 0, elements.length);
    IntIterator iter = _THE_HEAP_.orderedElements(true);
    final int[] returnThis = new int[iter.numRemaining()];
    for (int i = 0; i < returnThis.length; i++)
      returnThis[i] = iter.nextInt();
    return returnThis;
  }

}

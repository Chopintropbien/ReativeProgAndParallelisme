package reductions

import org.scalameter._
import common._
import scala.annotation.tailrec

// ce truc est faux!
object LineOfSight {

  val standardConfig = config(
    Key.exec.minWarmupRuns -> 40,
    Key.exec.maxWarmupRuns -> 80,
    Key.exec.benchRuns -> 100,
    Key.verbose -> true) withWarmer (new Warmer.Default)

  def max(a: Float, b: Float): Float = if (a > b) a else b

  def lineOfSight(input: Array[Float], output: Array[Float]): Unit = {
    @tailrec
    def lineOfSightAcc(i: Int, mx: Float): Unit = if (i < input.length) {
      val angle = max(input(i) / i, mx)
      output(i) = angle
      lineOfSightAcc(i + 1, angle)
    }

    // initialize 0 point  
    output(0) = 0
    lineOfSightAcc(1, 0);
  }

  sealed abstract class Tree {
    def maxPrevious: Float
  }
  case class Node(left: Tree, right: Tree) extends Tree {
    val maxPrevious = max(left.maxPrevious, right.maxPrevious)
  }
  case class Leaf(from: Int, until: Int, maxPrevious: Float) extends Tree

  /**
   * Traverses the specified part of the array and returns the maximum angle.
   */
  def upsweepSequential(input: Array[Float], from: Int, until: Int): Float = {
    def upsweepSeqAcc(i: Int, mx: Float): Float =
      if (i == until) mx
      else {
        val angle = max(input(i) / i, mx)
        upsweepSeqAcc(i + 1, angle)
      }
    upsweepSeqAcc(from, -1)
  }

  /**
   * Traverses the part of the array starting at `from` and until `end`, and
   *  returns the reduction tree for that part of the array.
   *
   *  The reduction tree is a `Leaf` if the length of the specified part of the
   *  array is longer than `threshold`, and a `Node` otherwise.
   *  If the specified part of the array is longer than `threshold`, then the
   *  work is divided and done recursively in parallel.
   */
  def upsweep(input: Array[Float], from: Int, end: Int, threshold: Int): Tree = {

    def upsweepNew(from: Int, end: Int, dist: Int): Tree = 
      if (end - from < threshold) {
        val maxPrevious = if(dist != 0) upsweepSequential(input, from - dist, end - dist) else -1
        new Leaf(from, end, maxPrevious)
      } 
      else{
        val mid = from + (end - from) / 2
        val (left, right) = parallel(upsweepNew(from, mid, dist / 2 + dist % 2), upsweepNew(mid, end, mid - from))
        new Node(left, right)
      }

    upsweepNew(from, end, 0)
  }

  /**
   * Traverses the part of the `input` array starting at `from` and until
   *  `end`, and computes the maximum angle for each entry of the output array,
   *  given the `startingAngle`.
   */
  @tailrec
  def downsweepSequential(input: Array[Float], output: Array[Float],
                          startingAngle: Float, from: Int, until: Int): Unit = if (from < until) {
    val mx = max(input(from) / from, startingAngle)
    output(from) = mx
    downsweepSequential(input, output, mx, from + 1, until)
  }

  /**
   * Pushes the maximum angle in the prefix of the array to each leaf of the
   *  reduction `tree` in parallel, and then calls `downsweepTraverse` to write
   *  the `output` angles.
   */
  def downsweep(input: Array[Float], output: Array[Float], startingAngle: Float,
                tree: Tree): Unit = tree match {
    case Leaf(from, until, maxPrevious) => downsweepSequential(input, output, max(maxPrevious, startingAngle), from, until)
    case Node(left, right) => parallel(downsweep(input, output, startingAngle, left),
      downsweep(input, output, startingAngle, right))
  }

  /** Compute the line-of-sight in parallel. */
  def parLineOfSight(input: Array[Float], output: Array[Float],
                     threshold: Int): Unit = {
    val tree = upsweep(input, 1, input.length, threshold)
    downsweep(input, output, 0, tree)
  }

  def main(args: Array[String]) {
    val length = 10000000
    val input = (0 until length).map(_ % 100 * 1.0f).toArray
    val output = new Array[Float](length + 1)
    val seqtime = standardConfig measure {
      lineOfSight(input, output)
    }
    println(s"sequential time: $seqtime ms")

    val partime = standardConfig measure {
      parLineOfSight(input, output, 10000)
    }
    println(s"parallel time: $partime ms")
    println(s"speedup: ${seqtime / partime}")
  }

}

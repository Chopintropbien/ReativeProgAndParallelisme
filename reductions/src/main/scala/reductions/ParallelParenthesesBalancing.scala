package reductions

import scala.annotation._
import org.scalameter._
import common._


object ParallelParenthesesBalancing {

  @volatile var seqResult = false

  @volatile var parResult = false

  val standardConfig = config(
    Key.exec.minWarmupRuns -> 40,
    Key.exec.maxWarmupRuns -> 80,
    Key.exec.benchRuns -> 120,
    Key.verbose -> true
  ) withWarmer(new Warmer.Default)

  def parToInt(c: Char): Int ={
      if(c == '(') 1
      else if(c == ')') -1
      else 0
    }
  
  /** Returns `true` iff the parentheses in the input `chars` are balanced.
   */
  def balance(chars: Array[Char]): Boolean = {
    def balanceList(chars: List[Char], acc: Int): Boolean = {
      if(acc == 0 && chars.isEmpty) true
      else if (acc < 0 || chars.isEmpty) false 
      else balanceList(chars.tail, acc + parToInt(chars.head))
    }
    balanceList(chars.toList, 0)
  }

  
  def seqBalance(chars: Array[Char], from: Int, until: Int): (Int, Int) = {
    // acc1: nb de parenthese ouverte n'ayant pas trouver de partenaire
    // acc2 nb parenthese fermer n'ayent pas trouver de partenaire
    def seqBalanceAcc(i: Int, acc1: Int, acc2: Int): (Int, Int) = {
      if(acc1 == -1)  seqBalanceAcc(i, 0, acc2 + 1)
      else if(i == until) (acc1, acc2)
      else seqBalanceAcc(i+1, acc1 + parToInt(chars(i)), acc2)
    }
    seqBalanceAcc(from, 0, 0)
  }
  
  
  
  /** Returns `true` iff the parentheses in the input `chars` are balanced.
   */
  def parBalance(chars: Array[Char], threshold: Int): Boolean = {
      
    def combine(a: (Int, Int), b: (Int, Int)): (Int, Int) = {
      val c = a._1 - b._2
      if(c > 0) (c + b._1, a._2)
      else (b._1, a._2 + Math.abs(c))
    }
    
    def traverse(from: Int, until: Int): (Int, Int) = {
      if(until - from < threshold) seqBalance(chars, from, until)
      else {
        val mid = from + (until - from) / 2
        val (a, b) = parallel(traverse(from, mid), traverse(mid, until))
        combine(a, b)
      }
    }
    
    traverse(0, chars.length) == (0,0)
  }

  // For those who want more:
  // Prove that your reduction operator is associative!

  def main(args: Array[String]): Unit = {
    val length = 100000000
    val chars = new Array[Char](length)
    val threshold = 10000
    val seqtime = standardConfig measure {
      seqResult = balance(chars)
    }
    println(s"sequential result = $seqResult")
    println(s"sequential balancing time: $seqtime ms")

    val fjtime = standardConfig measure {
      parResult = parBalance(chars, threshold)
    }
    println(s"parallel result = $parResult")
    println(s"parallel balancing time: $fjtime ms")
    println(s"speedup: ${seqtime / fjtime}")
  }

}

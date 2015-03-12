package reductions

import org.scalameter._
import common._

object ParallelCountChange {

  @volatile var seqResult = 0

  @volatile var parResult = 0

  val standardConfig = config(
    Key.exec.minWarmupRuns -> 20,
    Key.exec.maxWarmupRuns -> 40,
    Key.exec.benchRuns -> 80,
    Key.verbose -> true
  ) withWarmer(new Warmer.Default)

  /** Returns the number of ways change can be made from the specified list of
   *  coins for the specified amount of money.
   */
  def countChange(money: Int, coins: List[Int]): Int = {
    if(money == 0) 1
    else if(money < 0 || coins.isEmpty) 0 
    else countChange(money - coins.head, coins) + countChange(money, coins.tail)
  }

  type Threshold = (Int, List[Int]) => Boolean

  /** In parallel, counts the number of ways change can be made from the
   *  specified list of coins for the specified amount of money.
   */
  def parCountChange(money: Int, coins: List[Int], threshold: Threshold): Int = {
    if(threshold(money, coins) || coins.isEmpty) countChange(money, coins)
    else{
      val (right, left) = parallel(parCountChange(money - coins.head, coins, threshold), 
                                   parCountChange(money, coins.tail, threshold))
      right + left
    } 
  }

  /** Threshold heuristic based on the starting money. */
  def moneyThreshold(startingMoney: Int): Threshold =
    (money, _) => money <= startingMoney * 2.0 / 3.0

  /** Threshold heuristic based on the total number of initial coins. */
  def totalCoinsThreshold(totalCoins: Int): Threshold =
    (_, coins) => coins.length <= totalCoins * 2.0 / 3.0


  /** Threshold heuristic based on the starting money and the initial list of coins. */
  def combinedThreshold(startingMoney: Int, allCoins: List[Int]): Threshold = 
      (money, coins) => money * coins.length <= startingMoney * allCoins.length * 0.5
  

  def main(args: Array[String]): Unit = {
    val amount = 250
    val coins = List(1, 2, 5, 10, 20, 50)
    val seqtime = standardConfig measure {
      seqResult = countChange(amount, coins)
    }
    println(s"sequential result = $seqResult")
    println(s"sequential count time: $seqtime ms")

    def measureParallelCountChange(threshold: Threshold): Unit = {
      val fjtime = standardConfig measure {
        parResult = parCountChange(amount, coins, threshold)
      }
      println(s"parallel result = $parResult")
      println(s"parallel count time: $fjtime ms")
      println(s"speedup: ${seqtime / fjtime}")
    }

    measureParallelCountChange(moneyThreshold(amount))
    measureParallelCountChange(totalCoinsThreshold(coins.length))
    measureParallelCountChange(combinedThreshold(amount, coins))
  }

}
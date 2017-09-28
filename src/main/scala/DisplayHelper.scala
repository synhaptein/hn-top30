package com.p15x.hntop30

object DisplayHelper {
  // https://codegolf.stackexchange.com/questions/4707/outputting-ordinal-numbers-1st-2nd-3rd
  def ordinalNumber(n: Int) =
    n + { if (((n % 100) / 10) == 1) "th"
        else (("thstndrd"  + ("th"  * 6)).sliding (2, 2).toSeq (n % 10))
    }

  def fillsWith[T](list: List[T], filler: T, max: Int) = list.zipAll((1 to max), filler, 0).map(_._1)

  def createHeader(nbTopCommenters: Int) = "Story" :: (1 to nbTopCommenters).map(i => s"${ordinalNumber(i)} Top Commenter").toList
}

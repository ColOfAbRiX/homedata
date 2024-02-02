package com.colofabrix.scala.homedata.tado

import cats.implicits.given
import java.time.OffsetDateTime
import org.scalatest.flatspec.*
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import scala.collection.mutable.TreeMap
import scala.concurrent.duration.*

class TimeSlotsSpecs extends AnyFlatSpecLike with Matchers:

  "TimeSlots.add()" should "create an empty data" in {
    val actual = TimeSlotsM[Int](5.minutes).toSortedMap()
    actual shouldBe empty
  }

  it should "add a single element for a specific time" in {
    val actual =
      TimeSlotsM[Int](5.minutes)
        .add(odt"2024-02-02T11:17:20.037720600Z", 3)
        .toSortedMap()

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> 3,
      )

    actual shouldBe expected
  }

  it should "add different elements in different Time Slots in the correct order" in {
    val actual =
      TimeSlotsM[Int](5.minutes)
        .add(odt"2024-02-02T11:28:20.037720600Z", 3)
        .add(odt"2024-02-02T11:17:20.835607900Z", 9)
        .toSortedMap()

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> 9,
        odt"2024-02-02T11:25:00Z" -> 3,
      )

    actual shouldBe expected
  }

  it should "add combine elements that fall in the same Time Slot" in {
    val actual =
      TimeSlotsM[Int](5.minutes)
        .add(odt"2024-02-02T11:28:20.037720600Z", 3)
        .add(odt"2024-02-02T11:28:20.037720600Z", 5)
        .add(odt"2024-02-02T11:17:20.835607900Z", 1)
        .add(odt"2024-02-02T11:17:20.835607900Z", 2)
        .add(odt"2024-02-02T11:17:20.835607900Z", 9)
        .toSortedMap()

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> 12,
        odt"2024-02-02T11:25:00Z" -> 8,
      )

    actual shouldBe expected
  }

  "TimeSlots.combine()" should "combine an empty TimeSlot to an existing one and change nothing" in {
    val ts1 = TimeSlotsM[Int](5.minutes)
    val ts2 = TimeSlotsM[Int](5.minutes).add(odt"2024-02-02T11:17:20.037720600Z", 3)

    val actual = ts2.combine(ts1)

    actual shouldBe ts2
  }

  it should "combine two non-overlapping TimeSlots with the same resolution" in {
    val ts1 =
      TimeSlotsM[Int](5.minutes)
        .add(odt"2024-02-02T11:17:20.835607900Z", 1)
        .add(odt"2024-02-02T11:28:20.037720600Z", 2)

    val ts2 =
      TimeSlotsM[Int](5.minutes)
        .add(odt"2024-02-03T11:37:20.835607900Z", 3)
        .add(odt"2024-02-03T11:52:20.037720600Z", 4)

    val actual = ts1 combine ts2

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> 1,
        odt"2024-02-02T11:25:00Z" -> 2,
        odt"2024-02-03T11:35:00Z" -> 3,
        odt"2024-02-03T11:50:00Z" -> 4,
      )

    actual shouldBe expected
  }

  it should "combine two overlapping TimeSlots with the same resolution" in {
    val ts1 =
      TimeSlotsM[Int](5.minutes)
        .add(odt"2024-02-02T11:17:20.835607900Z", 1)
        .add(odt"2024-02-02T11:28:20.037720600Z", 2)

    val ts2 =
      TimeSlotsM[Int](5.minutes)
        .add(odt"2024-02-02T11:17:20.835607900Z", 3)
        .add(odt"2024-02-02T11:52:20.037720600Z", 5)

    val actual = ts1 combine ts2

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> 4,
        odt"2024-02-02T11:25:00Z" -> 2,
        odt"2024-02-02T11:50:00Z" -> 5,
      )

    actual shouldBe expected
  }

  it should "combine two TimeSlots with different resolution" in {
    val ts1 =
      TimeSlotsM[Int](5.minutes)
        .add(odt"2024-02-02T11:17:20.835607900Z", 1)
        .add(odt"2024-02-02T11:28:20.037720600Z", 2)

    val ts2 =
      TimeSlotsM[Int](3.minutes)
        .add(odt"2024-02-02T11:16:20.835607900Z", 3)
        .add(odt"2024-02-02T11:19:20.835607900Z", 5)
        .add(odt"2024-02-02T11:26:20.037720600Z", 8)

    val actual = ts1 combine ts2

    println(s"${ts1.show}\n")
    println(s"${ts2.show}\n")
    println(s"${actual.show}\n")

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> 4,
        odt"2024-02-02T11:25:00Z" -> 2,
        odt"2024-02-02T11:50:00Z" -> 5,
      )

    actual shouldBe expected
  }

  extension (sc: StringContext)
    def odt(args: Any*): OffsetDateTime = OffsetDateTime.parse(sc.parts.mkString)

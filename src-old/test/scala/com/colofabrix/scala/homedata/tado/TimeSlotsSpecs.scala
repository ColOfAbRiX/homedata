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
    val actual = TimeSlots[Int](5.minutes).toSlottedSortedMap
    actual shouldBe empty
  }

  it should "add a single element for a specific time" in {
    val actual =
      TimeSlots[Int](5.minutes)
        .add(odt"2024-02-02T11:17:20.037720600Z", 3)
        .toSlottedSortedMap

    val expected = TreeMap(odt"2024-02-02T11:15:00Z" -> Vector(3))

    actual shouldBe expected
  }

  it should "add different elements in different Time Slots in the correct order" in {
    val actual =
      TimeSlots[Int](5.minutes)
        .add(odt"2024-02-02T11:28:20.037720600Z", 3)
        .add(odt"2024-02-02T11:17:20.835607900Z", 9)
        .toSlottedSortedMap

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> Vector(9),
        odt"2024-02-02T11:25:00Z" -> Vector(3),
      )

    actual shouldBe expected
  }

  it should "add combine elements that fall in the same Time Slot" in {
    val actual =
      TimeSlots[Int](5.minutes)
        .add(odt"2024-02-02T11:28:20.037720600Z", 3)
        .add(odt"2024-02-02T11:28:20.037720600Z", 5)
        .add(odt"2024-02-02T11:17:20.835607900Z", 1)
        .add(odt"2024-02-02T11:17:20.835607900Z", 2)
        .add(odt"2024-02-02T11:17:20.835607900Z", 9)
        .toSlottedSortedMap

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> Vector(1, 2, 9),
        odt"2024-02-02T11:25:00Z" -> Vector(3, 5),
      )

    actual shouldBe expected
  }

  "TimeSlots.addToRange()" should "add a value to a single Time Slot when given the same to and from" in {
    val actual =
      TimeSlots[Int](5.minutes)
        .addToRange(
          odt"2024-02-02T11:17:20.037720600Z",
          odt"2024-02-02T11:17:20.037720600Z",
          3,
        )
        .toSlottedSortedMap

    val expected = TreeMap(odt"2024-02-02T11:15:00Z" -> Vector(3))

    actual shouldBe expected
  }

  it should "add a value to a single Time Slot" in {
    val actual =
      TimeSlots[Int](5.minutes)
        .addToRange(
          odt"2024-02-02T11:16:20.037720600Z",
          odt"2024-02-02T11:18:12.037720600Z",
          3,
        )
        .toSlottedSortedMap

    val expected = TreeMap(odt"2024-02-02T11:15:00Z" -> Vector(3))

    actual shouldBe expected
  }

  it should "add a value to multiple Time Slots not including the from-time" in {
    val actual =
      TimeSlots[Int](5.minutes)
        .addToRange(
          odt"2024-02-02T11:16:21Z",
          odt"2024-02-02T11:30:00Z",
          3,
        )
        .toSlottedSortedMap

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> Vector(3),
        odt"2024-02-02T11:20:00Z" -> Vector(3),
        odt"2024-02-02T11:25:00Z" -> Vector(3),
      )

    actual shouldBe expected
  }

  "TimeSlots.combine()" should "combine an empty TimeSlot to an existing one and change nothing" in {
    val ts1 = TimeSlots[Int](5.minutes)
    val ts2 = TimeSlots[Int](5.minutes).add(odt"2024-02-02T11:17:20.037720600Z", 3)

    val actual = ts2.combine(ts1)

    actual shouldBe ts2
  }

  it should "combine two non-overlapping TimeSlots with the same resolution" in {
    val ts1 =
      TimeSlots[Int](5.minutes)
        .add(odt"2024-02-02T11:17:20.835607900Z", 1)
        .add(odt"2024-02-02T11:28:20.037720600Z", 2)

    val ts2 =
      TimeSlots[Int](5.minutes)
        .add(odt"2024-02-03T11:37:20.835607900Z", 3)
        .add(odt"2024-02-03T11:52:20.037720600Z", 4)

    val actual = ts1 combine ts2

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> Vector(1),
        odt"2024-02-02T11:25:00Z" -> Vector(2),
        odt"2024-02-03T11:35:00Z" -> Vector(3),
        odt"2024-02-03T11:50:00Z" -> Vector(4),
      )

    actual shouldBe expected
  }

  it should "combine two overlapping TimeSlots with the same resolution" in {
    val ts1 =
      TimeSlots[Int](5.minutes)
        .add(odt"2024-02-02T11:17:20.835607900Z", 1)
        .add(odt"2024-02-02T11:28:20.037720600Z", 2)

    val ts2 =
      TimeSlots[Int](5.minutes)
        .add(odt"2024-02-02T11:17:20.835607900Z", 3)
        .add(odt"2024-02-02T11:52:20.037720600Z", 5)

    val actual = ts1 combine ts2

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> Vector(4),
        odt"2024-02-02T11:25:00Z" -> Vector(2),
        odt"2024-02-02T11:50:00Z" -> Vector(5),
      )

    actual shouldBe expected
  }

  it should "combine two TimeSlots with different resolution" in {
    val ts1 =
      TimeSlots[Int](5.minutes)
        .add(odt"2024-02-02T11:17Z", 1) // Time slot minutes [15, 20)
        .add(odt"2024-02-02T11:28Z", 2) // Time slot minutes [25, 30)

    val ts2 =
      TimeSlots[Int](3.minutes)
        .add(odt"2024-02-02T11:16Z", 3) // Time slot minutes [15, 18) maps into ts1[15, 20)
        .add(odt"2024-02-02T11:19Z", 5) // Time slot minutes [18, 21) maps into ts1[15, 20) U ts1[20, 25)
        .add(odt"2024-02-02T11:26Z", 8) // Time slot minutes [24, 27) maps into ts1[20, 25) U ts1[25, 30)

    val actual = ts1 combine ts2

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> Vector((1 + 3 + 5)),
        odt"2024-02-02T11:20:00Z" -> Vector((5 + 8)),
        odt"2024-02-02T11:25:00Z" -> Vector((2 + 8)),
      )

    actual shouldBe expected
  }

  "TimeSlots.toSortedMap()" should "return the underlying Map without any time gap" in {
    val actual =
      TimeSlots[Int](5.minutes)
        .add(odt"2024-02-02T11:17:00Z", 1)
        .add(odt"2024-02-02T11:28:00Z", 3)
        .add(odt"2024-02-02T11:31:00Z", 2)
        .add(odt"2024-02-02T11:46:00Z", 4)
        .toSortedMap

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> Vector(1),
        odt"2024-02-02T11:20:00Z" -> Vector(0),
        odt"2024-02-02T11:25:00Z" -> Vector(0),
        odt"2024-02-02T11:25:00Z" -> Vector(3),
        odt"2024-02-02T11:30:00Z" -> Vector(2),
        odt"2024-02-02T11:35:00Z" -> Vector(0),
        odt"2024-02-02T11:40:00Z" -> Vector(0),
        odt"2024-02-02T11:45:00Z" -> Vector(4),
      )

    actual shouldBe expected
  }

  extension (sc: StringContext)
    def odt(args: Any*): OffsetDateTime = OffsetDateTime.parse(sc.parts.mkString)

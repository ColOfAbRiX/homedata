package com.colofabrix.scala.tado4s.store

import com.colofabrix.scala.tado4s.store.TimeSlots.*
import java.time.OffsetDateTime
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import scala.collection.mutable.TreeMap
import scala.concurrent.duration.*

class TimeSlotsSpecs extends AnyFlatSpecLike with Matchers:

  "TimeSlots.add()" should "create an empty data" in {
    val actual = TimeSlots[Int](5.minutes).toRawMap
    actual shouldBe empty
  }

  it should "add a single element for a specific time" in {
    val value1 = (odt"2024-02-02T11:17:20.037720600Z", 3)

    val actual =
      TimeSlots[Int](5.minutes)
        .add.tupled(value1)
        .toRawMap

    val expected = TreeMap(odt"2024-02-02T11:15:00Z" -> instantValues(value1))

    actual shouldBe expected
  }

  it should "add different instantaneous values in different Time Slots in the correct order" in {
    val value1 = (odt"2024-02-02T11:28:20.0377206Z", 3)
    val value2 = (odt"2024-02-02T11:17:20.8356079Z", 9)

    val actual =
      TimeSlots[Int](5.minutes)
        .add.tupled(value1)
        .add.tupled(value2)
        .toRawMap

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> instantValues(value2),
        odt"2024-02-02T11:25:00Z" -> instantValues(value1),
      )

    actual shouldBe expected
  }

  it should "add place together instantaneous values that fall in the same Time Slot" in {
    val value1 = (odt"2024-02-02T11:28:20.0377206Z", 3)
    val value2 = (odt"2024-02-02T11:28:20.0377206Z", 5)
    val value3 = (odt"2024-02-02T11:17:20.8356079Z", 1)
    val value4 = (odt"2024-02-02T11:17:20.8356079Z", 2)
    val value5 = (odt"2024-02-02T11:17:20.8356079Z", 9)

    val actual =
      TimeSlots[Int](5.minutes)
        .add.tupled(value1)
        .add.tupled(value2)
        .add.tupled(value3)
        .add.tupled(value4)
        .add.tupled(value5)
        .toRawMap

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> instantValues(value3, value4, value5),
        odt"2024-02-02T11:25:00Z" -> instantValues(value1, value2),
      )

    actual shouldBe expected
  }

  "TimeSlots.add()" should "add a Time Span value to a single Time Slot when given the same to and from" in {
    val value1 = (odt"2024-02-02T11:17:20.0377206Z", odt"2024-02-02T11:17:20.0377206Z", 3)

    val actual =
      TimeSlots[Int](5.minutes)
        .add.tupled(value1)
        .toRawMap

    val expected = TreeMap(odt"2024-02-02T11:15:00Z" -> instantValues(value1.drop(1)))

    actual shouldBe expected
  }

  it should "add a Time Span value to a single Time Slot" in {
    val value1 = (odt"2024-02-02T11:16:20.0377206Z", odt"2024-02-02T11:18:12.0377206Z", 3)

    val actual =
      TimeSlots[Int](5.minutes)
        .add.tupled(value1)
        .toRawMap

    val expected = TreeMap(odt"2024-02-02T11:15:00Z" -> timeSpanValues(value1))

    actual shouldBe expected
  }

  it should "add a Time Span value when the to and from arguments are reversed" in {
    val value1 = (odt"2024-02-02T11:18:12.0377206Z", odt"2024-02-02T11:16:20.0377206Z", 3)

    val actual =
      TimeSlots[Int](5.minutes)
        .add.tupled(value1)
        .toRawMap

    val expected = TreeMap(odt"2024-02-02T11:15:00Z" -> timeSpanValues(value1))

    actual shouldBe expected
  }

  it should "add a time Span value to a single Time Slots, including the from-time and excluding the to-time" in {
    val value1 = (odt"2024-02-02T11:15:00Z", odt"2024-02-02T11:20:00Z", 3)

    val actual =
      TimeSlots[Int](5.minutes)
        .add.tupled(value1)
        .toRawMap

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> timeSpanValues(value1),
      )

    actual shouldBe expected
  }

  it should "add a time Span value to multiple Time Slots, including the from-time and excluding the to-time" in {
    val value1 = (odt"2024-02-02T11:15:00Z", odt"2024-02-02T11:25:00Z", 3)

    val actual =
      TimeSlots[Int](5.minutes)
        .add.tupled(value1)
        .toRawMap

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> timeSpanValues(value1),
        odt"2024-02-02T11:20:00Z" -> timeSpanValues(value1),
      )

    actual shouldBe expected
  }

  it should "add a time Span value to multiple Time Slots, including the from-time and excluding the to-time, on disconnected time slots" in {
    val value1 = (odt"2024-02-02T11:15:00Z", odt"2024-02-02T11:25:00Z", 3)
    val value2 = (odt"2024-02-02T11:30:00Z", odt"2024-02-02T11:35:00Z", 3)

    val actual =
      TimeSlots[Int](5.minutes)
        .add.tupled(value1)
        .add.tupled(value2)
        .toRawMap

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> timeSpanValues(value1),
        odt"2024-02-02T11:20:00Z" -> timeSpanValues(value1),
        odt"2024-02-02T11:30:00Z" -> timeSpanValues(value2),
      )

    actual shouldBe expected
  }

  "TimeSlots.combine()" should "combine an empty TimeSlot to an existing one and change nothing" in {
    val ts1 = TimeSlots[Int](5.minutes)
    val ts2 = TimeSlots[Int](5.minutes).add(odt"2024-02-02T11:17:20.0377206Z", 3)

    val actual = ts2.combine(ts1)

    actual shouldBe ts2
  }

  it should "combine two non-overlapping TimeSlots with the same resolution" in {
    val value1 = (odt"2024-02-02T11:17:20.8356079Z", 1)
    val value2 = (odt"2024-02-02T11:28:20.0377206Z", 2)
    val value3 = (odt"2024-02-03T11:37:20.8356079Z", 3)
    val value4 = (odt"2024-02-03T11:52:20.0377206Z", 4)

    val ts1 =
      TimeSlots[Int](5.minutes)
        .add.tupled(value1)
        .add.tupled(value2)

    val ts2 =
      TimeSlots[Int](5.minutes)
        .add.tupled(value3)
        .add.tupled(value4)

    val actual = (ts1 combine ts2).toRawMap

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> instantValues(value1),
        odt"2024-02-02T11:25:00Z" -> instantValues(value2),
        odt"2024-02-03T11:35:00Z" -> instantValues(value3),
        odt"2024-02-03T11:50:00Z" -> instantValues(value4),
      )

    actual shouldBe expected
  }

  it should "combine two overlapping TimeSlots with the same resolution" in {
    val value1 = (odt"2024-02-02T11:17:20.8356079Z", 1)
    val value2 = (odt"2024-02-02T11:28:20.0377206Z", 2)
    val value3 = (odt"2024-02-02T11:17:20.8356079Z", 3)
    val value4 = (odt"2024-02-02T11:52:20.0377206Z", 4)

    val ts1 =
      TimeSlots[Int](5.minutes)
        .add.tupled(value1)
        .add.tupled(value2)

    val ts2 =
      TimeSlots[Int](5.minutes)
        .add.tupled(value3)
        .add.tupled(value4)

    val actual = (ts1 combine ts2).toRawMap

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> instantValues(value1, value3),
        odt"2024-02-02T11:25:00Z" -> instantValues(value2),
        odt"2024-02-02T11:50:00Z" -> instantValues(value4),
      )

    actual shouldBe expected
  }

  it should "combine two TimeSlots of instantaneous values with different resolution a be non-commutative" in {
    val value1 = (odt"2024-02-02T11:17Z", 1)
    val value2 = (odt"2024-02-02T11:28Z", 2)
    val value3 = (odt"2024-02-02T11:16Z", 3)
    val value4 = (odt"2024-02-02T11:19Z", 4)
    val value5 = (odt"2024-02-02T11:26Z", 5)

    val ts1 =
      TimeSlots[Int](5.minutes)
        .add.tupled(value1)
        .add.tupled(value2)

    val ts2 =
      TimeSlots[Int](3.minutes)
        .add.tupled(value3)
        .add.tupled(value4)
        .add.tupled(value5)

    val actual1 = (ts1 combine ts2)

    val expected1 =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> instantValues(value1, value3, value4),
        odt"2024-02-02T11:25:00Z" -> instantValues(value2, value5),
      )

    actual1.resolution shouldBe ts1.resolution
    actual1.toRawMap shouldBe expected1

    val actual2 = (ts2 combine ts1)

    val expected2 =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> instantValues(value3, value1),
        odt"2024-02-02T11:18:00Z" -> instantValues(value4),
        odt"2024-02-02T11:24:00Z" -> instantValues(value5),
        odt"2024-02-02T11:27:00Z" -> instantValues(value2),
      )

    actual2.resolution shouldBe ts2.resolution
    actual2.toRawMap shouldBe expected2
  }

  it should "combine two TimeSlots of time span values with different resolution" in {
    val value1 = (odt"2024-02-02T11:17Z", 1)
    val value2 = (odt"2024-02-02T11:28Z", 2)
    val value3 = (odt"2024-02-02T11:16Z", 3)
    val value4 = (odt"2024-02-02T11:19Z", 4)
    val value5 = (odt"2024-02-02T11:26Z", 5)

    val ts1 =
      TimeSlots[Int](5.minutes)
        .add.tupled(value1)
        .add.tupled(value2)

    val ts2 =
      TimeSlots[Int](3.minutes)
        .add.tupled(value3)
        .add.tupled(value4)
        .add.tupled(value5)

    val actual1 = (ts1 combine ts2)

    val expected1 =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> instantValues(value1, value3, value4),
        odt"2024-02-02T11:25:00Z" -> instantValues(value2, value5),
      )

    actual1.resolution shouldBe ts1.resolution
    actual1.toRawMap shouldBe expected1

    val actual2 = (ts2 combine ts1)

    val expected2 =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> instantValues(value3, value1),
        odt"2024-02-02T11:18:00Z" -> instantValues(value4),
        odt"2024-02-02T11:24:00Z" -> instantValues(value5),
        odt"2024-02-02T11:27:00Z" -> instantValues(value2),
      )

    actual2.resolution shouldBe ts2.resolution
    actual2.toRawMap shouldBe expected2
  }

  "TimeSlots.toMap()" should "return the underlying Map without any gap of missing time slots" in {
    val value1 = (odt"2024-02-02T11:17:00Z", 1)
    val value2 = (odt"2024-02-02T11:28:00Z", 3)
    val value3 = (odt"2024-02-02T11:31:00Z", 2)
    val value4 = (odt"2024-02-02T11:46:00Z", 4)

    val actual =
      TimeSlots[Int](5.minutes)
        .add.tupled(value1)
        .add.tupled(value2)
        .add.tupled(value3)
        .add.tupled(value4)
        .toMap

    val expected =
      TreeMap(
        odt"2024-02-02T11:15:00Z" -> instantValues(value1),
        odt"2024-02-02T11:20:00Z" -> instantValues(),
        odt"2024-02-02T11:25:00Z" -> instantValues(),
        odt"2024-02-02T11:25:00Z" -> instantValues(value2),
        odt"2024-02-02T11:30:00Z" -> instantValues(value3),
        odt"2024-02-02T11:35:00Z" -> instantValues(),
        odt"2024-02-02T11:40:00Z" -> instantValues(),
        odt"2024-02-02T11:45:00Z" -> instantValues(value4),
      )

    actual shouldBe expected
  }

  "TimeSlots.setResolution()" should "move an Instantaneous value in the correct Time Slot when upscaling" in {
    val value1 = (odt"2024-02-02T11:17:00Z", 1)
    val value2 = (odt"2024-02-02T11:13:00Z", 2)
    val value3 = (odt"2024-02-02T11:28:00Z", 3)
    val value4 = (odt"2024-02-02T11:29:00Z", 4)

    val ts =
      TimeSlots[Int](10.minutes)
        .add.tupled(value1)
        .add.tupled(value2)
        .add.tupled(value3)
        .add.tupled(value4)

    val actual = ts.setResolution(5.minutes)

    val expected =
      TreeMap(
        odt"2024-02-02T11:10:00Z" -> instantValues(value2),
        odt"2024-02-02T11:15:00Z" -> instantValues(value1),
        odt"2024-02-02T11:25:00Z" -> instantValues(value3, value4),
      )

    actual.resolution shouldBe 5.minutes
    actual.toRawMap shouldBe expected
  }

  it should "move an Instantaneous value in the correct Time Slot when downscaling" in {
    val value1 = (odt"2024-02-02T11:17:00Z", 1)
    val value2 = (odt"2024-02-02T11:13:00Z", 2)
    val value3 = (odt"2024-02-02T11:28:00Z", 3)
    val value4 = (odt"2024-02-02T11:29:00Z", 4)

    val ts =
      TimeSlots[Int](5.minutes)
        .add.tupled(value1)
        .add.tupled(value2)
        .add.tupled(value3)
        .add.tupled(value4)

    val actual = ts.setResolution(10.minutes)

    val expected =
      TreeMap(
        odt"2024-02-02T11:10:00Z" -> instantValues(value2, value1),
        odt"2024-02-02T11:20:00Z" -> instantValues(value3, value4),
      )

    actual.resolution shouldBe 10.minutes
    actual.toRawMap shouldBe expected
  }

  it should "move an Time Span value in the correct Time Slot when upscaling" in {
    val value1 = (odt"2024-02-02T11:17:00Z", odt"2024-02-02T11:18:00Z", 1)
    val value2 = (odt"2024-02-02T11:13:00Z", odt"2024-02-02T11:18:00Z", 2)
    val value3 = (odt"2024-02-02T11:13:00Z", odt"2024-02-02T11:28:00Z", 3)
    val value4 = (odt"2024-02-02T11:29:00Z", odt"2024-02-02T11:29:00Z", 4)

    val ts =
      TimeSlots[Int](10.minutes)
        .add.tupled(value1)
        .add.tupled(value2)
        .add.tupled(value3)
        .add.tupled(value4)

    val actual = ts.setResolution(5.minutes)

    val expected =
      TreeMap(
        odt"2024-02-02T11:10:00Z" -> timeSpanValues(value2, value3),
        odt"2024-02-02T11:15:00Z" -> timeSpanValues(value1, value2, value3),
        odt"2024-02-02T11:20:00Z" -> timeSpanValues(value3),
        odt"2024-02-02T11:25:00Z" -> (timeSpanValues(value3) ++ instantValues(value4.drop(1))),
      )

    actual.resolution shouldBe 5.minutes
    actual.toRawMap shouldBe expected
  }

  it should "move an Time Span value in the correct Time Slot when downscaling" in {
    val value1 = (odt"2024-02-02T11:17:00Z", odt"2024-02-02T11:18:00Z", 1)
    val value2 = (odt"2024-02-02T11:13:00Z", odt"2024-02-02T11:18:00Z", 2)
    val value3 = (odt"2024-02-02T11:13:00Z", odt"2024-02-02T11:28:00Z", 3)
    val value4 = (odt"2024-02-02T11:29:00Z", odt"2024-02-02T11:29:00Z", 4)

    val ts =
      TimeSlots[Int](5.minutes)
        .add.tupled(value1)
        .add.tupled(value2)
        .add.tupled(value3)
        .add.tupled(value4)

    val actual = ts.setResolution(10.minutes)

    val expected =
      TreeMap(
        odt"2024-02-02T11:10:00Z" -> timeSpanValues(value1, value2, value3),
        odt"2024-02-02T11:20:00Z" -> (timeSpanValues(value3) ++ instantValues(value4.drop(1))),
      )

    actual.resolution shouldBe 10.minutes
    actual.toRawMap shouldBe expected
  }

  private def instantValues(values: (OffsetDateTime, Int)*): Set[TimeValue[Int]] =
    values
      .toSet
      .map { case (time, value) => TimeValue.InstantValue(time, value) }

  private def timeSpanValues(values: (OffsetDateTime, OffsetDateTime, Int)*): Set[TimeValue[Int]] =
    values
      .toSet
      .map { case (from, to, value) => TimeValue.TimeSpanValue(from, to, value) }

  extension (sc: StringContext)
    def odt(args: Any*): OffsetDateTime = OffsetDateTime.parse(sc.parts.mkString)

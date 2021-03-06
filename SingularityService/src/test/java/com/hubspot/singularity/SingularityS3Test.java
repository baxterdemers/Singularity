package com.hubspot.singularity;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.TimeZone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SingularityS3Test {

  @Test
  public void testS3FormatHelper() throws Exception {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

    SingularityTaskId taskId = new SingularityTaskId("rid", "did", 1, 1, "host", "rack");

    long start = 1414610537117L; // Wed, 29 Oct 2014 19:22:17 GMT
    long end = 1415724215000L; // Tue, 11 Nov 2014 16:43:35 GMT

    Collection<String> prefixes = SingularityS3FormatHelper.getS3KeyPrefixes(
      "%Y/%m/%taskId",
      taskId,
      Optional.<String>empty(),
      start,
      end,
      "default",
      Collections.emptyList()
    );

    Assertions.assertTrue(prefixes.size() == 2);

    end = 1447265861000L; // Tue, 11 Nov 2015 16:43:35 GMT

    prefixes =
      SingularityS3FormatHelper.getS3KeyPrefixes(
        "%Y/%taskId",
        taskId,
        Optional.<String>empty(),
        start,
        end,
        "default",
        Collections.emptyList()
      );

    Assertions.assertTrue(prefixes.size() == 2);

    start = 1415750399999L;
    end = 1415771999000L;

    prefixes =
      SingularityS3FormatHelper.getS3KeyPrefixes(
        "%Y/%m/%d/%taskId",
        taskId,
        Optional.<String>empty(),
        start,
        end,
        "default",
        Collections.emptyList()
      );

    Assertions.assertTrue(prefixes.size() == 2);

    prefixes =
      SingularityS3FormatHelper.getS3KeyPrefixes(
        "%requestId/%group/%Y/%m",
        taskId,
        Optional.<String>empty(),
        start,
        end,
        "groupName",
        Collections.emptyList()
      );

    Assertions.assertEquals("rid/groupName/2014/11", prefixes.iterator().next());

    final long NOV2014TUES11 = 1415724215000L;

    Assertions.assertEquals(
      "wat-hostname",
      SingularityS3FormatHelper.getKey(
        "wat-%host",
        0,
        System.currentTimeMillis(),
        "filename",
        "hostname"
      )
    );
    Assertions.assertEquals(
      "file1.txt-2",
      SingularityS3FormatHelper.getKey(
        "%filename-%index",
        2,
        System.currentTimeMillis(),
        "file1.txt",
        "hostname"
      )
    );
    Assertions.assertEquals(
      "yo-2014-11-" + NOV2014TUES11 + "-.txt",
      SingularityS3FormatHelper.getKey(
        "yo-%Y-%m-%s-%fileext",
        2,
        NOV2014TUES11,
        "file1.txt",
        "hostname"
      )
    );

    String guid = SingularityS3FormatHelper.getKey(
      "yo-%guid",
      2,
      NOV2014TUES11,
      "file1.txt",
      "hostname"
    );

    Assertions.assertTrue(guid.startsWith("yo-"));

    Assertions.assertTrue(guid.length() > 10);

    String guid2 = SingularityS3FormatHelper.getKey(
      "yo-%guid",
      2,
      NOV2014TUES11,
      "file1.txt",
      "hostname"
    );

    Assertions.assertTrue(!guid.equals(guid2));
  }
}

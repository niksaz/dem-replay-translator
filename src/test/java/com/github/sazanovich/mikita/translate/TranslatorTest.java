package com.github.sazanovich.mikita.translate;

import static org.junit.Assert.*;

import org.junit.Test;

public class TranslatorTest {

  @Test
  public void testParseNumpyIntArray() {
    String textNumpyIntArray = "[1,0,1,1,1,0,0,0]";
    int[] expectedResult = {1, 0, 1, 1, 1, 0, 0, 0};

    int[] result = Translator.parseNumpyIntArray(textNumpyIntArray);
    assertArrayEquals(expectedResult, result);
  }
}
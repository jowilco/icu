﻿/*
 *******************************************************************************
 * Copyright (C) 1996-2011, Google, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

/**
 * Provide a base class for Transforms that focuses just on the transformation of the text. APIs that take Transliterator, but only depend on the text transformation should use this interface in the API instead.
 *
 * @stable ICU 3.8
 * @author markdavis
 *
 */
public interface StringTransform extends Transform<String,String> {
    /**
     * Transform the text in some way, to be determined by the subclass.
     * @param source text to be transformed (eg lowercased)
     * @return result
     * @stable ICU 3.8
     */
    public String transform(String source);
}
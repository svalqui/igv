/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.broad.igv.tools.sort;

import org.broad.igv.feature.tribble.MUTCodec;
import htsjdk.tribble.readers.AsciiLineReader;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author jrobinso
 *         Date: 4/8/13
 *         Time: 4:23 PM
 */
public class MUTSorter extends AsciiSorter {

    int chrCol;
    int startCol;


    public MUTSorter(File inputFile, File outputFile) {

        super(inputFile, outputFile);
        MUTCodec codec = new MUTCodec(inputFile.getAbsolutePath(), null);
        chrCol = codec.getChrColumn();
        startCol = codec.getStartColumn();
    }


    @Override
    Parser getParser() throws IOException {

        return new Parser(chrCol, startCol);
    }

    @Override
    String writeHeader(AsciiLineReader reader, PrintWriter writer) throws IOException {

        String nextLine;
        // TODO -- check "browser" line syntax,  is it a multi-line directive?
        while ((nextLine = reader.readLine()).startsWith("#")) {
            writer.println(nextLine);   // Comments, directives
        }
        writer.println(nextLine);  // Header

        // Contract is to return the first data line!
        nextLine = reader.readLine();

        return nextLine;


    }
}

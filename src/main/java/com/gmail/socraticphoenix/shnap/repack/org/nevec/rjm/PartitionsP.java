/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 socraticphoenix@gmail.com
 * Copyright (c) 2017 contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 socraticphoenix@gmail.com
 * Copyright (c) 2017 contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.gmail.socraticphoenix.shnap.repack.org.nevec.rjm;

import java.lang.* ;
import java.util.* ;
import java.math.* ;

/** Number of partitions.
* @since 2008-10-15
* @author Richard J. Mathar
*/
public class PartitionsP
{
        /**
        * The list of all partitions as a vector.
        */
        static protected Vector<BigInteger> a = new Vector<BigInteger>() ;

        /**
        * The maximum integer covered by the high end of the list.
        */
        static protected BigInteger nMax =new BigInteger("-1") ;

        /**
        * Default constructor initializing a list of partitions up to 7.
        */
        public PartitionsP()
        {
                if ( a.size() == 0 )
                {
                        a.add(new BigInteger(""+1)) ;
                        a.add(new BigInteger(""+1)) ;
                        a.add(new BigInteger(""+2)) ;
                        a.add(new BigInteger(""+3)) ;
                        a.add(new BigInteger(""+5)) ;
                        a.add(new BigInteger(""+7)) ;
                }
                nMax = new BigInteger(""+(a.size()-1)) ;
        } /* ctor */

        /** return the number of partitions of i
        * @param i the zero-based index into the list of partitions
        * @return the ith partition number. This is 1 if i=0 or 1, 2 if i=2 and so forth.
        */
        public BigInteger at(int i)
        {
                /* If the current list is too small, increase in intervals
                * of 3 until the list has at least i elements.
                */
                while ( i > nMax.intValue() )
                {
                        growto(nMax.add(new BigInteger(""+3))) ;
                }
                return ( a.elementAt(i) ) ;
        } /* at */

        /** extend the list of known partitions up to n
        * @param n the maximum integer hashed after the call.
        */
        private void growto(BigInteger n)
        {
                while( a.size() <= n.intValue() )
                {
                        BigInteger per = new BigInteger("0") ;
                        BigInteger cursiz = new BigInteger(""+a.size()) ;
                        for(int k=0; k < a.size() ; k++)
                        {
                                BigInteger tmp = a.elementAt(k).multiply(BigIntegerMath.sigma(a.size()-k)) ;
                                per = per.add(tmp) ;
                        }
                        a.add(per.divide(cursiz)) ;
                }
                nMax = new BigInteger(""+(a.size()-1)) ;
        } /* growto */

        /** Test program.
        * It takes one integer argument n and prints P(n).<br>
        * java -cp . PartitionsP n<br>
        * @since 2008-10-15
        */
        public static void main(String[] args) throws Exception
        {
                PartitionsP a = new PartitionsP() ;
                int n = (new Integer(args[0])).intValue() ;
                System.out.println("P("+ n +")=" + a.at(n)) ;
        }
}

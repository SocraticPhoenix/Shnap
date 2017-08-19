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

import java.util.* ;
import java.math.* ;

/** Euler numbers
* @see <a href="http://oeis.org/A000364">A000364</a> in the OEIS.
* @since 2008-10-30
* @author Richard J. Mathar
*/
public class Euler
{
        /*
        * The list of all Euler numbers as a vector, n=0,2,4,....
        */
        static protected Vector<BigInteger> a = new Vector<BigInteger>() ;

        /** Ctor(). Fill the hash list initially with E_0 to E_3.
        */
        public Euler()
        {
                if ( a.size() == 0 )
                {
                        a.add(BigInteger.ONE) ;
                        a.add(BigInteger.ONE) ;
                        a.add(new BigInteger("5")) ;
                        a.add(new BigInteger("61")) ;
                }
        }

        /** Compute a coefficient in the internal table.
        * @param n the zero-based index of the coefficient. n=0 for the E_0 term. 
        */
        protected void set(final int n)
        {
                while ( n >= a.size())
                {
                        BigInteger val = BigInteger.ZERO ;
                        boolean sigPos = true; 
                        int thisn = a.size() ;
                        for(int i= thisn-1 ; i > 0 ; i--)
                        {
                                BigInteger f = new BigInteger(""+ a.elementAt(i).toString() ) ;
                                f = f.multiply( BigIntegerMath.binomial(2*thisn,2*i) );
                                if ( sigPos )
                                        val = val.add(f) ;
                                else
                                        val = val.subtract(f) ;
                                sigPos = ! sigPos ;
                        }
                        if ( thisn % 2 ==0 )
                                val = val.subtract(BigInteger.ONE) ;
                        else
                                val = val.add(BigInteger.ONE) ;
                        a.add(val) ;
                }
        }

        /** The Euler number at the index provided.
        * @param n the index, non-negative.
        * @return the E_0=E_1=1 , E_2=5, E_3=61 etc
        */
        public BigInteger at(int n)
        {
                set(n) ;
                return(a.elementAt(n)) ;
        }

} /* Euler */

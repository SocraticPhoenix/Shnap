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
import java.math.* ;

/** Euler totient function.
* @see <a href="http://oeis.org/A000010">A000010</a> in the OEIS.
* @since 2008-10-14
* @since 2012-03-04 Adapted to new Ifactor representation.
* @author Richard J. Mathar
*/
public class EulerPhi
{
        /** Default constructor.
        * Does nothing().
        */
        public EulerPhi()
        {
        }

        /** Compute phi(n).
        * @param n The positive argument of the function.
        * @return phi(n)
        */
        public BigInteger at(int n)
        {
                return at(new BigInteger(""+n) ) ;
        } /* at */

        /** Compute phi(n).
        * @param n The positive argument of the function.
        * @return phi(n)
        */
        public BigInteger at(BigInteger n)
        {
                if ( n.compareTo(BigInteger.ZERO) <= 0 )
                        throw new ArithmeticException("negative argument "+n+ " of EulerPhi") ;
                Ifactor prFact = new Ifactor(n) ;
                BigInteger phi = n ;
                if ( n.compareTo(BigInteger.ONE) > 0 )
                        for(int i=0 ; i < prFact.primeexp.size() ; i += 2)
                        {
                                BigInteger p = new BigInteger(prFact.primeexp.elementAt(i).toString()) ;
                                BigInteger p_1 = p.subtract(BigInteger.ONE) ;
                                phi = phi.multiply(p_1).divide(p) ;
                        }
                return phi ;
        } /* at */

        /** Test program.
        * It takes one argument n and prints the value phi(n).<br>
        * java -cp . EulerPhi n<br>
        * @since 2006-08-14
        */
        public static void main(String[] args) throws ArithmeticException
        {
                EulerPhi a = new EulerPhi() ;
                int n = (new Integer(args[0])).intValue() ;
                System.out.println("phi("+ n + ") = " + a.at(n)) ;
        }
} /* EulerPhi */

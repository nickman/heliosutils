// Copyright (c) 2013 Darach Ennis < darach at gmail dot com >.
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions:  
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.heliosapm.eep;

/**
 * <p>Title: WallClock</p>
 * <p>Description: </p> 
 * <p>Copied and renamed from <a href="https://github.com/darach/eep-java">eep-java</a>.
 * <p>Copyright retained by author and header included above as requested</p>
 * @author 2013 Darach Ennis < darach at gmail dot com >
 * <p><code>com.heliosapm.eep.WallClock</code></p>
 */
public class WallClock implements EventClock<Long> {
	private Long at;
	private Long interval;
	private long mark;

	public WallClock(long intervalNanos) {
		interval = intervalNanos;
	}
	
	public Long at() {
		return at;
	}

	public Long init() {
		at = System.nanoTime();
		mark = at + interval;
		return at;
	}

	public Long inc() {
		return at = System.nanoTime();
	}

	public boolean tick() {
		return ((at - mark) >= interval);
	}

	public boolean tock(Long elapsed) {
		final long d = at - elapsed;
		if (d >= interval) {
			mark += interval;
			return true;
		}
		return false;
	}
}

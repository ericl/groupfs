package groupfs.tests;

import groupfs.storage.*;
import groupfs.state.Manager;

import fuse.FuseException;

import groupfs.*;

// shows necessity of fine-grained invalidation of groups
public class LargeScaleValidation extends Test {
	public void run() {
		Manager backend = getNewBackend();
		syn(backend, "1.jpg", "Concert", "Art");
		syn(backend, "2.jpg", "Concert", "Art");
		syn(backend, "3.jpg", "Concert", "Art");
		syn(backend, "4.jpg", "Concert", "Portrait", "Family");
		syn(backend, "5.jpg", "Concert", "Portrait", "Family");
		syn(backend, "6.jpg", "Concert", "Portrait", "Eric");
		syn(backend, "7.jpg", "Concert", "Water");
		syn(backend, "8.jpg", "Concert", "Architecture");
		syn(backend, "9.jpg", "Festival", "Competition");
		syn(backend, "10.jpg", "Festival", "Portrait", "Family");
		syn(backend, "11.jpg", "Festival", "Portrait", "Eric");
		Filesystem fs = new Filesystem(backend);
		expect(fs,
			new String[] {
				"./.jpg/1.jpg",
				"./.jpg/10.jpg",
				"./.jpg/11.jpg",
				"./.jpg/2.jpg",
				"./.jpg/3.jpg",
				"./.jpg/4.jpg",
				"./.jpg/5.jpg",
				"./.jpg/6.jpg",
				"./.jpg/7.jpg",
				"./.jpg/8.jpg",
				"./.jpg/9.jpg",
				"./Architecture/8.jpg",
				"./Art/1.jpg",
				"./Art/2.jpg",
				"./Art/3.jpg",
				"./Competition/9.jpg",
				"./Concert/1.jpg",
				"./Concert/2.jpg",
				"./Concert/3.jpg",
				"./Concert/4.jpg",
				"./Concert/5.jpg",
				"./Concert/6.jpg",
				"./Concert/7.jpg",
				"./Concert/8.jpg",
				"./Concert/Architecture/8.jpg",
				"./Concert/Art/1.jpg",
				"./Concert/Art/2.jpg",
				"./Concert/Art/3.jpg",
				"./Concert/Eric/6.jpg",
				"./Concert/Family/4.jpg",
				"./Concert/Family/5.jpg",
				"./Concert/Portrait/4.jpg",
				"./Concert/Portrait/5.jpg",
				"./Concert/Portrait/6.jpg",
				"./Concert/Portrait/Eric/6.jpg",
				"./Concert/Portrait/Family/4.jpg",
				"./Concert/Portrait/Family/5.jpg",
				"./Concert/Water/7.jpg",
				"./Eric/11.jpg",
				"./Eric/6.jpg",
				"./Eric/Concert/6.jpg",
				"./Eric/Festival/11.jpg",
				"./Family/10.jpg",
				"./Family/4.jpg",
				"./Family/5.jpg",
				"./Family/Concert/4.jpg",
				"./Family/Concert/5.jpg",
				"./Family/Festival/10.jpg",
				"./Festival/10.jpg",
				"./Festival/11.jpg",
				"./Festival/9.jpg",
				"./Festival/Competition/9.jpg",
				"./Festival/Eric/11.jpg",
				"./Festival/Family/10.jpg",
				"./Festival/Portrait/10.jpg",
				"./Festival/Portrait/11.jpg",
				"./Festival/Portrait/Eric/11.jpg",
				"./Festival/Portrait/Family/10.jpg",
				"./Portrait/10.jpg",
				"./Portrait/11.jpg",
				"./Portrait/4.jpg",
				"./Portrait/5.jpg",
				"./Portrait/6.jpg",
				"./Portrait/Concert/4.jpg",
				"./Portrait/Concert/5.jpg",
				"./Portrait/Concert/6.jpg",
				"./Portrait/Concert/Eric/6.jpg",
				"./Portrait/Concert/Family/4.jpg",
				"./Portrait/Concert/Family/5.jpg",
				"./Portrait/Eric/11.jpg",
				"./Portrait/Eric/6.jpg",
				"./Portrait/Eric/Concert/6.jpg",
				"./Portrait/Eric/Festival/11.jpg",
				"./Portrait/Family/10.jpg",
				"./Portrait/Family/4.jpg",
				"./Portrait/Family/5.jpg",
				"./Portrait/Family/Concert/4.jpg",
				"./Portrait/Family/Concert/5.jpg",
				"./Portrait/Family/Festival/10.jpg",
				"./Portrait/Festival/10.jpg",
				"./Portrait/Festival/11.jpg",
				"./Portrait/Festival/Eric/11.jpg",
				"./Portrait/Festival/Family/10.jpg",
				"./Water/7.jpg",
			},
			new String[] {
				".",
				"./.jpg",
				"./Architecture",
				"./Art",
				"./Competition",
				"./Concert",
				"./Concert/Architecture",
				"./Concert/Art",
				"./Concert/Eric",
				"./Concert/Family",
				"./Concert/Portrait",
				"./Concert/Portrait/Eric",
				"./Concert/Portrait/Family",
				"./Concert/Water",
				"./Eric",
				"./Eric/Concert",
				"./Eric/Festival",
				"./Family",
				"./Family/Concert",
				"./Family/Festival",
				"./Festival",
				"./Festival/Competition",
				"./Festival/Eric",
				"./Festival/Family",
				"./Festival/Portrait",
				"./Festival/Portrait/Eric",
				"./Festival/Portrait/Family",
				"./Portrait",
				"./Portrait/Concert",
				"./Portrait/Concert/Eric",
				"./Portrait/Concert/Family",
				"./Portrait/Eric",
				"./Portrait/Eric/Concert",
				"./Portrait/Eric/Festival",
				"./Portrait/Family",
				"./Portrait/Family/Concert",
				"./Portrait/Family/Festival",
				"./Portrait/Festival",
				"./Portrait/Festival/Eric",
				"./Portrait/Festival/Family",
				"./Water",
			}
		);
		try {
			fs.rename("/Concert/Portrait/5.jpg", "/Concert/Portrait/Eric/5.jpg");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./.jpg/1.jpg",
				"./.jpg/10.jpg",
				"./.jpg/11.jpg",
				"./.jpg/2.jpg",
				"./.jpg/3.jpg",
				"./.jpg/4.jpg",
				"./.jpg/5.jpg",
				"./.jpg/6.jpg",
				"./.jpg/7.jpg",
				"./.jpg/8.jpg",
				"./.jpg/9.jpg",
				"./Architecture/8.jpg",
				"./Art/1.jpg",
				"./Art/2.jpg",
				"./Art/3.jpg",
				"./Competition/9.jpg",
				"./Concert/1.jpg",
				"./Concert/2.jpg",
				"./Concert/3.jpg",
				"./Concert/4.jpg",
				"./Concert/5.jpg",
				"./Concert/6.jpg",
				"./Concert/7.jpg",
				"./Concert/8.jpg",
				"./Concert/Architecture/8.jpg",
				"./Concert/Art/1.jpg",
				"./Concert/Art/2.jpg",
				"./Concert/Art/3.jpg",
				"./Concert/Eric/5.jpg",
				"./Concert/Eric/6.jpg",
				"./Concert/Eric/Family/5.jpg",
				"./Concert/Family/4.jpg",
				"./Concert/Family/5.jpg",
				"./Concert/Family/Eric/5.jpg",
				"./Concert/Portrait/4.jpg",
				"./Concert/Portrait/5.jpg",
				"./Concert/Portrait/6.jpg",
				"./Concert/Portrait/Eric/5.jpg",
				"./Concert/Portrait/Eric/6.jpg",
				"./Concert/Portrait/Eric/Family/5.jpg",
				"./Concert/Portrait/Family/4.jpg",
				"./Concert/Portrait/Family/5.jpg",
				"./Concert/Portrait/Family/Eric/5.jpg",
				"./Concert/Water/7.jpg",
				"./Eric/11.jpg",
				"./Eric/5.jpg",
				"./Eric/6.jpg",
				"./Eric/Concert/5.jpg",
				"./Eric/Concert/6.jpg",
				"./Eric/Concert/Family/5.jpg",
				"./Eric/Family/5.jpg",
				"./Eric/Festival/11.jpg",
				"./Family/10.jpg",
				"./Family/4.jpg",
				"./Family/5.jpg",
				"./Family/Concert/4.jpg",
				"./Family/Concert/5.jpg",
				"./Family/Concert/Eric/5.jpg",
				"./Family/Eric/5.jpg",
				"./Family/Festival/10.jpg",
				"./Festival/10.jpg",
				"./Festival/11.jpg",
				"./Festival/9.jpg",
				"./Festival/Competition/9.jpg",
				"./Festival/Eric/11.jpg",
				"./Festival/Family/10.jpg",
				"./Festival/Portrait/10.jpg",
				"./Festival/Portrait/11.jpg",
				"./Festival/Portrait/Eric/11.jpg",
				"./Festival/Portrait/Family/10.jpg",
				"./Portrait/10.jpg",
				"./Portrait/11.jpg",
				"./Portrait/4.jpg",
				"./Portrait/5.jpg",
				"./Portrait/6.jpg",
				"./Portrait/Concert/4.jpg",
				"./Portrait/Concert/5.jpg",
				"./Portrait/Concert/6.jpg",
				"./Portrait/Concert/Eric/5.jpg",
				"./Portrait/Concert/Eric/6.jpg",
				"./Portrait/Concert/Eric/Family/5.jpg",
				"./Portrait/Concert/Family/4.jpg",
				"./Portrait/Concert/Family/5.jpg",
				"./Portrait/Concert/Family/Eric/5.jpg",
				"./Portrait/Eric/11.jpg",
				"./Portrait/Eric/5.jpg",
				"./Portrait/Eric/6.jpg",
				"./Portrait/Eric/Concert/5.jpg",
				"./Portrait/Eric/Concert/6.jpg",
				"./Portrait/Eric/Concert/Family/5.jpg",
				"./Portrait/Eric/Family/5.jpg",
				"./Portrait/Eric/Festival/11.jpg",
				"./Portrait/Family/10.jpg",
				"./Portrait/Family/4.jpg",
				"./Portrait/Family/5.jpg",
				"./Portrait/Family/Concert/4.jpg",
				"./Portrait/Family/Concert/5.jpg",
				"./Portrait/Family/Concert/Eric/5.jpg",
				"./Portrait/Family/Eric/5.jpg",
				"./Portrait/Family/Festival/10.jpg",
				"./Portrait/Festival/10.jpg",
				"./Portrait/Festival/11.jpg",
				"./Portrait/Festival/Eric/11.jpg",
				"./Portrait/Festival/Family/10.jpg",
				"./Water/7.jpg",
			},
			new String[] {
				".",
				"./.jpg",
				"./Architecture",
				"./Art",
				"./Competition",
				"./Concert",
				"./Concert/Architecture",
				"./Concert/Art",
				"./Concert/Eric",
				"./Concert/Eric/Family",
				"./Concert/Family",
				"./Concert/Family/Eric",
				"./Concert/Portrait",
				"./Concert/Portrait/Eric",
				"./Concert/Portrait/Eric/Family",
				"./Concert/Portrait/Family",
				"./Concert/Portrait/Family/Eric",
				"./Concert/Water",
				"./Eric",
				"./Eric/Concert",
				"./Eric/Concert/Family",
				"./Eric/Family",
				"./Eric/Festival",
				"./Family",
				"./Family/Concert",
				"./Family/Concert/Eric",
				"./Family/Eric",
				"./Family/Festival",
				"./Festival",
				"./Festival/Competition",
				"./Festival/Eric",
				"./Festival/Family",
				"./Festival/Portrait",
				"./Festival/Portrait/Eric",
				"./Festival/Portrait/Family",
				"./Portrait",
				"./Portrait/Concert",
				"./Portrait/Concert/Eric",
				"./Portrait/Concert/Eric/Family",
				"./Portrait/Concert/Family",
				"./Portrait/Concert/Family/Eric",
				"./Portrait/Eric",
				"./Portrait/Eric/Concert",
				"./Portrait/Eric/Concert/Family",
				"./Portrait/Eric/Family",
				"./Portrait/Eric/Festival",
				"./Portrait/Family",
				"./Portrait/Family/Concert",
				"./Portrait/Family/Concert/Eric",
				"./Portrait/Family/Eric",
				"./Portrait/Family/Festival",
				"./Portrait/Festival",
				"./Portrait/Festival/Eric",
				"./Portrait/Festival/Family",
				"./Water",
			}
		);
	}
}

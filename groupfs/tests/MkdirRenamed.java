package groupfs.tests;

import fuse.FuseException;

import groupfs.backend.*;

import groupfs.*;

// shows new dirs showing up (correctly) in multiple directories because of the cache
public class MkdirRenamed extends Test {
	public void run() {
		DataProvider backend = getNewBackend();
		syn(backend, "fish recipe.txt", "fish", "recipe");
		syn(backend, "pizza recipe.txt", "recipe");
		syn(backend, "fish picture.png", "fish");
		Filesystem fs = new Filesystem(backend);
		expect(fs,
			new String[] {
				"./fish/fish recipe.txt",
				"./fish/recipe/fish recipe.txt",
				"./fish/fish picture.png",
				"./fish/.png/fish picture.png",
				"./fish/.txt/fish recipe.txt",
				"./recipe/fish/fish recipe.txt",
				"./recipe/fish recipe.txt",
				"./recipe/pizza recipe.txt",
				"./.txt/fish recipe.txt",
				"./.txt/pizza recipe.txt",
				"./.png/fish picture.png",
			},
			new String[] {
				".",
				"./fish",
				"./fish/recipe",
				"./fish/.png",
				"./fish/.txt",
				"./recipe",
				"./recipe/fish",
				"./.txt",
				"./.png",
			}
		);
		try {
			fs.mkdir("/fish/recipe/untitled folder", 0);
			fs.rename("/fish/recipe/untitled folder", "/fish/recipe/new");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect_nocopy(fs,
			new String[] {
				"./fish/fish recipe.txt",
				"./fish/recipe/fish recipe.txt",
				"./fish/fish picture.png",
				"./fish/.png/fish picture.png",
				"./fish/.txt/fish recipe.txt",
				"./recipe/fish/fish recipe.txt",
				"./recipe/fish recipe.txt",
				"./recipe/pizza recipe.txt",
				"./.txt/fish recipe.txt",
				"./.txt/pizza recipe.txt",
				"./.png/fish picture.png",
			},
			new String[] {
				".",
				"./fish",
				"./fish/recipe",
				"./fish/recipe/new",
				"./fish/.png",
				"./fish/.txt",
				"./recipe",
				"./recipe/fish",
				"./recipe/fish/new",
				"./.txt",
				"./.png",
			}
		);
		try {
			fs.mkdir("/fish/recipe/fish", 0);
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect_nocopy(fs,
			new String[] {
				"./fish/fish recipe.txt",
				"./fish/recipe/fish recipe.txt",
				"./fish/fish picture.png",
				"./fish/.png/fish picture.png",
				"./fish/.txt/fish recipe.txt",
				"./recipe/fish/fish recipe.txt",
				"./recipe/fish recipe.txt",
				"./recipe/pizza recipe.txt",
				"./.txt/fish recipe.txt",
				"./.txt/pizza recipe.txt",
				"./.png/fish picture.png",
			},
			new String[] {
				".",
				"./fish",
				"./fish/recipe",
				"./fish/recipe/new",
				"./fish/.png",
				"./fish/.txt",
				"./recipe",
				"./recipe/fish",
				"./recipe/fish/new",
				"./.txt",
				"./.png",
			}
		);
		try {
			fs.mkdir("/foobar", 0);
			fs.rename("/foobar", "/s!!!!");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect_nocopy(fs,
			new String[] {
				"./fish/fish recipe.txt",
				"./fish/recipe/fish recipe.txt",
				"./fish/fish picture.png",
				"./fish/.png/fish picture.png",
				"./fish/.txt/fish recipe.txt",
				"./recipe/fish/fish recipe.txt",
				"./recipe/fish recipe.txt",
				"./recipe/pizza recipe.txt",
				"./.txt/fish recipe.txt",
				"./.txt/pizza recipe.txt",
				"./.png/fish picture.png",
			},
			new String[] {
				".",
				"./s!!!!",
				"./fish",
				"./fish/recipe",
				"./fish/recipe/new",
				"./fish/.png",
				"./fish/.txt",
				"./recipe",
				"./recipe/fish",
				"./recipe/fish/new",
				"./.txt",
				"./.png",
			}
		);
	}
}

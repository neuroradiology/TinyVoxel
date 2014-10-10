TinyVoxel
=========

A voxelrenderer made with LibGDX supporting desktop, Android and browser. iOS is untested. Ouya support is buggy, due to the small depth buffer.

Everything is GLES 2.0 compatible and there are no extensions used.

See an example of a HTML build at http://www.vrpaint.com/voxel .

Usage
-----
Create a bundle and add it to the transparentBundleArray or bundleArray. You can do transformations on the 'transform' matrix, 
but be sure to call 'updateMatrix()' afterwards.

Physics
-------
Currently there are a few physics functions; collision with a sphere, collision with a ray or collision with a point. 
Except for collision with a point, the collisions have some margin of error (due to the boundingbox being used to test).
Sphere and point collisions are pretty fast.

Transparency
------------
Transparency is currently hardcoded. Note that transparent objects behind other transparent object aren't visible. 

Transparency takes a bit more visual processing.

Scaling
-------
Isn't incorporated at the moment, but could be in the future. Biggest hurdle is the rendering front to back.

Character
---------
The character script uses two spheres to test for collisions. If the character is stuck, it moves upwards 
until it is free.

Controls
--------
I hacked in my own way for usage of the right mouse button under GWT. It doesn't always work fully yet however.

GUI and Menu
------------
These are singleton objects and are currently called mostly from the Game class.

Voxel size
----------
To have compatibility with HTML, I choose a voxel density of 8. Mostly, paint speed is affected with higher voxel density. 
On desktop, a voxel density of 16 is very much possible.

Technique
---------
This engine uses a combination of raycasting and geometric voxels. To stay GLES 2.0 compatible (and not uses fragDepth), 
there are some depth inconsistencies, when different grids collide.

Import
------
Currently the loading falls back to import. Importing of binvox models is supported. Importing of mesh (g3dj, g3db or obj) models is supported.

#!/usr/bin/python
"""
Copyright (C) 2003  John-Paul Gignac

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
"""

# Import Modules
import os, pygame, random, time, math, re, sys, md5
from pygame.locals import *

# Parse the command line
highscores_file = "pathological_scores"
screenshot = 0
fullscreen = 0
colorblind = 0
sound_on = 1
music_on = 1
for arg in sys.argv[1:]:
	if arg == '-s':
		screenshot = 1
	elif arg == '-f':
		fullscreen = 1
	elif arg == '-cb':
		colorblind = 1
	elif arg == '-q':
		sound_on = 0
		music_on = 0
	elif arg[0] == '-':
		print "Usage: "+sys.argv[0]+" [-cb] [-f] [-s] [highscores-file]\n"
		sys.exit(1)
	else:
		highscores_file = arg

if colorblind:
	cbext = '-cb.png'
else:
	cbext = '.png'

# The location of the setgid script for writing highscores
# This script is only used if the highscores file is not writable directly
write_highscores = "/usr/lib/pathological/bin/write-highscores"

# Game constants
wheel_steps = 9
frames_per_sec = 100
timer_width = 36
timer_margin = 4
info_height = 20
initial_lives = 3
extra_life_frequency = 5000 # Extra life awarded every this many points
max_spare_lives = 10

# Volume levels
intro_music_volume = 0.6
ingame_music_volume = 0.9
sound_effects_volume = 0.6

# Changing these may affect the playability of levels
default_colors = (2,3,4,6)  # Blue, Green, Yellow, Red
default_stoplight = (6,4,3) # Red, Yellow, Green
default_launch_timer = 6    # 6 passes
default_board_timer = 30    # 30 seconds per wheel
marble_speed = 2            # Marble speed in pixels/frame (must be 1, 2 or 4)
trigger_time = 30           # 30 seconds
replicator_delay = 35       # 35 frames

# Don't change these constants unless you
# redo all of the levels
horiz_tiles = 8
vert_tiles = 6

# Don't change these constants unless you
# update the graphics files correspondingly.
screen_width = 800
screen_height = 600
marble_size = 28
tile_size = 92
wheel_margin = 4
stoplight_marble_size = 28
life_marble_size = 16

# The positions of the holes in the wheels in
# each of the three rotational positions
holecenter_radius = (tile_size - marble_size) / 2 - wheel_margin
holecenters = []
for i in range(wheel_steps):
	theta = math.pi * i / (2 * wheel_steps)
	c = math.floor( 0.5 + math.cos(theta)*holecenter_radius)
	s = math.floor( 0.5 + math.sin(theta)*holecenter_radius)
	holecenters.append((
		(tile_size/2 + s, tile_size/2 - c),
		(tile_size/2 + c, tile_size/2 + s),
		(tile_size/2 - s, tile_size/2 + c),
		(tile_size/2 - c, tile_size/2 - s)))

# Direction references
dirs = ((0,-1),(1,0),(0,1),(-1,0))

# More global variables
board_width = horiz_tiles * tile_size
board_height = vert_tiles * tile_size
launch_timer_pos = (0,info_height)
board_pos = (timer_width, info_height + marble_size)
timer_height = board_height + marble_size
music_loaded = 0

# Functions to create our resources
def load_image(name, colorkey=-1, size=None):
	fullname = os.path.join('graphics', name)
	try:
		image = pygame.image.load(fullname)
	except pygame.error, message:
		print 'Cannot load image:', fullname
		raise SystemExit, message

	if size is not None:
		image = pygame.transform.scale( image, size)
	image = image.convert()

	if colorkey is not None:
		if colorkey is -1:
			colorkey = image.get_at((0,0))
		image.set_colorkey(colorkey, RLEACCEL)
	return image

def load_sound(name, volume=1.0):
	class NoneSound:
		def play(self): pass
	if not pygame.mixer or not pygame.mixer.get_init():
		return NoneSound()
	fullname = os.path.join('sounds', name)
	try:
		sound = pygame.mixer.Sound(fullname)
	except pygame.error, message:
		print 'Cannot load sound:', fullname
		return NoneSound()

	sound.set_volume( volume * sound_effects_volume)

	return sound

def play_sound(sound):
	if sound_on: sound.play()

def start_music(name, volume=-1):
	global music_pending_song, music_loaded, music_volume

	music_volume = volume

	if not music_on:
		music_pending_song = name
		return

	if not pygame.mixer or not pygame.mixer.music:
		print "Background music not available."
		return
	fullname = os.path.join('music', name)
	try:
		pygame.mixer.music.load(fullname)
	except pygame.error, message:
		print 'Cannot load music:', fullname
		return
	music_loaded = 1
	pygame.mixer.music.play(-1)

	if music_volume >= 0:
		pygame.mixer.music.set_volume( music_volume)

	music_pending_song = 0

def toggle_fullscreen():
	global fullscreen
	if pygame.display.toggle_fullscreen():
		fullscreen = fullscreen ^ 1
		return 1
	else:
		return 0

def toggle_sound():
	global sound_on
	sound_on = sound_on ^ 1

def toggle_music():
	global music_on
	music_on = music_on ^ 1
	if music_on:
		if music_pending_song:
			start_music( music_pending_song)
		elif music_loaded:
			pygame.mixer.music.unpause()
	elif music_loaded:
		if not music_pending_song:
			pygame.mixer.music.pause()

# A better tick function
next_frame = pygame.time.get_ticks()
def my_tick( frames_per_sec):
	global next_frame
	# Wait for the next frame
	next_frame += 1000.0 / frames_per_sec
	now = pygame.time.get_ticks()
	if next_frame < now:
		# No time to wait - just hide our mistake
		# and keep going as fast as we can.
		next_frame = now
	else:
		pygame.time.wait( int(next_frame) - now)

# Load the sounds
def load_sounds():
	global filter_admit,wheel_turn,wheel_completed,change_color
	global direct_marble,ping,trigger_setup,teleport,marble_release
	global levelfinish,die,incorrect,switch,shredder,replicator
	global extra_life,menu_scroll,menu_select
	
	filter_admit = load_sound('filter_admit.wav', 0.8)
	wheel_turn = load_sound('wheel_turn.wav', 0.8)
	wheel_completed = load_sound('wheel_completed.wav', 0.7)
	change_color = load_sound('change_color.wav', 0.8)
	direct_marble = load_sound('direct_marble.wav', 0.6)
	ping = load_sound('ping.wav', 0.8)
	trigger_setup = load_sound('trigger_setup.wav')
	teleport = load_sound('teleport.wav', 0.6)
	marble_release = load_sound('marble_release.wav', 0.5)
	levelfinish = load_sound('levelfinish.wav', 0.6)
	die = load_sound('die.wav')
	incorrect = load_sound('incorrect.wav', 0.15)
	switch = load_sound('switch.wav')
	shredder = load_sound('shredder.wav')
	replicator = load_sound('replicator.wav')
	extra_life = load_sound('extra_life.wav')
	menu_scroll = load_sound('menu_scroll.wav', 0.8)
	menu_select = load_sound('switch.wav')

# Load the fonts for various parts of the game
def load_fonts():
	global launch_timer_font,active_marbles_font,popup_font,info_font

	launch_timer_font = pygame.font.Font(None, timer_width - 2*timer_margin)
	active_marbles_font = pygame.font.Font(None, marble_size)
	popup_font = pygame.font.Font(None, 24)
	info_font = pygame.font.Font(None, info_height)

# Load all of the images for the various game classes.
# The images are stored as class variables in the corresponding classes.
def load_images():
	Marble.images = []
	for i in range(9):
		Marble.images.append( load_image('marble-'+`i`+cbext, -1,
			(marble_size, marble_size)))

	Tile.plain_tiles = []
	Tile.tunnels = []
	for i in range(16):
		tile = load_image('tile.png', (206,53,53), (tile_size,tile_size))
		path = load_image('path-'+`i`+'.png', -1, (tile_size,tile_size))
		tile.blit( path, (0,0))
		Tile.plain_tiles.append( tile)
		Tile.tunnels.append(load_image('tunnel-'+`i`+'.png',
			-1,(tile_size,tile_size)))
	Tile.paths = 0

	Wheel.images = (
		load_image('wheel.png',-1,(tile_size,tile_size)),
		load_image('wheel-dark.png',-1,(tile_size, tile_size)),
		)
	Wheel.blank_images = (
		load_image('blank-wheel.png',-1,(tile_size,tile_size)),
		load_image('blank-wheel-dark.png',-1,(tile_size, tile_size)),
		)
	Wheel.moving_holes = (
		load_image('moving-hole.png',-1,(marble_size,marble_size)),
		load_image('moving-hole-dark.png',-1,(marble_size, marble_size)),
		)

	Buffer.bottom = load_image('buffer.png',-1,(tile_size,tile_size))
	Buffer.top = load_image('buffer-top.png',-1,(tile_size,tile_size))

	Painter.images = []
	for i in range(8):
		Painter.images.append( load_image('painter-'+`i`+cbext, -1,
			(tile_size,tile_size)))

	Filter.images = []
	for i in range(8):
		Filter.images.append( load_image('filter-'+`i`+cbext, -1,
			(tile_size,tile_size)))

	Director.images = (
		load_image('director-0.png',-1,(tile_size,tile_size)),
		load_image('director-1.png',-1,(tile_size,tile_size)),
		load_image('director-2.png',-1,(tile_size,tile_size)),
		load_image('director-3.png',-1,(tile_size,tile_size)),
		)

	Shredder.image = load_image('shredder.png',-1,(tile_size,tile_size))

	Switch.images = []
	for i in range(4):
		Switch.images.append( [])
		for j in range(4):
			if i == j: Switch.images[i].append( None)
			else: Switch.images[i].append( load_image(
				'switch-'+`i`+`j`+'.png',-1,(tile_size,tile_size)))

	Replicator.image = load_image('replicator.png',-1,(tile_size,tile_size))

	Teleporter.image_h = load_image('teleporter-h.png',-1,(tile_size,tile_size))
	Teleporter.image_v = load_image('teleporter-v.png',-1,(tile_size,tile_size))

	Trigger.image = load_image('trigger.png',-1,(tile_size,tile_size))

	Stoplight.image = load_image('stoplight.png',-1,(tile_size,tile_size))
	Stoplight.smallmarbles = []
	for im in Marble.images:
		Stoplight.smallmarbles.append( pygame.transform.scale(im,
			(stoplight_marble_size,stoplight_marble_size)))

	Board.life_marble = load_image('life-marble.png', -1,
		(life_marble_size, life_marble_size))
	Board.launcher_background = load_image('launcher.png', None,
		(horiz_tiles * tile_size,marble_size))
	Board.launcher_v = load_image('launcher-v.png', None,
		(marble_size, vert_tiles * tile_size + marble_size))
	Board.launcher_corner = load_image('launcher-corner.png', (255,0,0),
		((tile_size-marble_size)/2+marble_size,marble_size))
	Board.launcher_entrance = load_image('entrance.png', -1,
		(tile_size,marble_size))

	IntroScreen.background = load_image('intro.png', None,
		(screen_width, screen_height))
	IntroScreen.menu_font = pygame.font.Font(
		None, IntroScreen.menu_font_height)
	IntroScreen.scroller_font = pygame.font.Font(
		None, IntroScreen.scroller_font_height)
	IntroScreen.hs_font = pygame.font.Font(
		None, IntroScreen.hs_font_height)

# Function to set the video mode
def set_video_mode():
	global screen

	icon = pygame.image.load(os.path.join('graphics','icon.png'))
	icon.set_colorkey(icon.get_at((0,0)), RLEACCEL)
	pygame.display.set_icon(icon) # Needed both before and after set_mode
	screen = pygame.display.set_mode( (screen_width, screen_height),
		fullscreen * FULLSCREEN)
	pygame.display.set_icon(icon) # Needed both before and after set_mode
	pygame.display.set_caption('Pathological')

# Classes for our game objects

class Marble:
	def __init__(self, color, center, direction):
		self.color = color
		self.rect = pygame.Rect((0,0,marble_size,marble_size))
		self.rect.center = center
		self.direction = direction

	def update(self, board):
		self.rect.move_ip(
			marble_speed * dirs[self.direction][0],
			marble_speed * dirs[self.direction][1])

		board.affect_marble( self)

	def undraw(self, screen, background):
		screen.set_clip( self.rect)
		screen.blit( background, (0,0))
		screen.set_clip()

	def draw(self, screen):
		screen.blit( self.images[self.color], self.rect.topleft)

class Tile:
	def __init__(self, paths=0, center=None):
		self.paths = paths

		if center is None:
			center = (0,0)

		self.center = center
		self.rect = pygame.Rect((0,0,tile_size,tile_size))
		self.rect.center = center
		self.drawn = 0

	def draw_back(self, surface):
		if self.drawn: return 0
		surface.blit( self.plain_tiles[self.paths], self.rect.topleft)
		self.drawn = 1
		return 1

	def update(self, board): pass

	def draw_fore(self, surface): return 0

	def click(self, board, posx, posy, tile_x, tile_y): pass

	def affect_marble(self, board, marble, rpos):
		if rpos == (tile_size/2,tile_size/2):
			if self.paths & (1 << marble.direction): return

			# Figure out the new direction
			t = self.paths - (1 << (marble.direction^2))
			if t == 1: marble.direction = 0
			elif t == 2: marble.direction = 1
			elif t == 4: marble.direction = 2
			elif t == 8: marble.direction = 3
			else: marble.direction = marble.direction ^ 2

class Wheel(Tile):
	def __init__(self, paths, center=None):
		Tile.__init__(self, paths, center) # Call base class intializer
		self.spinpos = 0
		self.completed = 0
		self.marbles = [ -3, -3, -3, -3 ]

	def draw_back(self, surface):
		if self.drawn: return 0

		Tile.draw_back(self, surface)

		if self.spinpos:
			surface.blit( self.blank_images[self.completed], self.rect.topleft)
			for i in range(4):
				holecenter = holecenters[self.spinpos][i]
				surface.blit( self.moving_holes[self.completed],
					(holecenter[0]-marble_size/2+self.rect.left,
					holecenter[1]-marble_size/2+self.rect.top))
		else:
			surface.blit( self.images[self.completed], self.rect.topleft)

		for i in range(4):
			color = self.marbles[i]
			if color >= 0:
				holecenter = holecenters[self.spinpos][i]
				surface.blit( Marble.images[color],
					(holecenter[0]-marble_size/2+self.rect.left,
					holecenter[1]-marble_size/2+self.rect.top))

		return 1

	def update(self, board):
		if self.spinpos > 0:
			self.spinpos -= 1
			self.drawn = 0

	def click(self, board, posx, posy, tile_x, tile_y):
		# Ignore all clicks while rotating
		if self.spinpos: return

		b1, b2, b3 = pygame.mouse.get_pressed()
		if b3:
			# First, make sure that no marbles are currently entering
			for i in self.marbles:
				if i == -1 or i == -2: return

			# Start the wheel spinning
			self.spinpos = wheel_steps - 1
			play_sound( wheel_turn)

			# Reposition the marbles
			t = self.marbles[0]
			self.marbles[0] = self.marbles[1]
			self.marbles[1] = self.marbles[2]
			self.marbles[2] = self.marbles[3]
			self.marbles[3] = t

			self.drawn = 0

		elif b1:
			# Determine which hole is being clicked
			for i in range(4):
				# If there is no marble here, skip it
				if self.marbles[i] < 0: continue

				holecenter = holecenters[0][i]
				rect = pygame.Rect( 0, 0, marble_size, marble_size)
				rect.center = holecenter
				if rect.collidepoint( posx, posy):

					# Determine the neighboring tile
					neighbor = board.tiles[ (tile_y + dirs[i][1]) %
						vert_tiles][ (tile_x + dirs[i][0]) % horiz_tiles]

					if (
						# Disallow marbles to go off the top of the board
						(tile_y == 0 and i==0) or

						# If there is no way out here, skip it
						((self.paths & (1 << i)) == 0) or

						# If the neighbor is a wheel that is either turning
						# or has a marble already in the hole, disallow
						# the ejection
						(isinstance(neighbor, Wheel) and
						(neighbor.spinpos or
						neighbor.marbles[i^2] != -3))
						):
						play_sound( incorrect)
					else:
						# If the neighbor is a wheel, apply a special lock
						if isinstance(neighbor, Wheel):
							neighbor.marbles[i^2] = -2
						elif len(board.marbles) >= board.live_marbles_limit:
							# Impose the live marbles limit
							play_sound( incorrect)
							break

						# Eject the marble
						board.marbles.append(
							Marble( self.marbles[i],
							(holecenter[0]+self.rect.left,
							holecenter[1]+self.rect.top),
							i))
						self.marbles[i] = -3
						play_sound( marble_release)
						self.drawn = 0

					break

	def affect_marble(self, board, marble, rpos):
		# Watch for marbles entering
		if rpos[0]+marble_size/2 == wheel_margin or \
			rpos[0]-marble_size/2 == tile_size - wheel_margin or \
			rpos[1]+marble_size/2 == wheel_margin or \
			rpos[1]-marble_size/2 == tile_size - wheel_margin:
			if self.spinpos or self.marbles[marble.direction^2] >= -1:
				# Reject the marble
				marble.direction = marble.direction ^ 2
				play_sound( ping)
			else:
				self.marbles[marble.direction^2] = -1

		for holecenter in holecenters[0]:
			if rpos == holecenter:
				# Accept the marble
				board.marbles.remove( marble)
				self.marbles[marble.direction^2] = marble.color

				self.drawn = 0

				break

	def complete(self, board):
		# Complete the wheel
		for i in range(4): self.marbles[i] = -3
		if self.completed: board.game.increase_score( 10)
		else: board.game.increase_score( 50)
		self.completed = 1
		play_sound( wheel_completed)
		self.drawn = 0

	def maybe_complete(self, board):
		if self.spinpos > 0: return 0

		# Is there a trigger?
		if (board.trigger is not None) and \
			(board.trigger.marbles is not None):
			# Compare against the trigger
			for i in range(4):
				if self.marbles[i] != board.trigger.marbles[i] and \
					self.marbles[i] != 8: return 0
			self.complete( board)
			board.trigger.complete( board)
			return 1

		# Do we have four the same color?
		color = 8
		for c in self.marbles:
			if c < 0: return 0
			if color==8: color=c
			elif c==8: c=color
			elif c != color: return 0

		# Is there a stoplight?
		if (board.stoplight is not None) and \
			(board.stoplight.current < 3):
			# Compare against the stoplight
			if color != 8 and \
				color != board.stoplight.marbles[board.stoplight.current]:
				return 0
			else:
				board.stoplight.complete( board)

		self.complete( board)
		return 1

class Buffer(Tile):
	def __init__(self, paths, color=-1):
		Tile.__init__(self, paths) # Call base class intializer
		self.marble = color
		self.entering = None

	def draw_back(self, surface):
		if self.drawn: return 0

		Tile.draw_back(self, surface)

		color = self.marble
		if color >= 0:
			holecenter = self.rect.center
			surface.blit( Marble.images[color],
				(holecenter[0]-marble_size/2,
				holecenter[1]-marble_size/2))
		else:
			surface.blit( self.bottom, self.rect.topleft)


		return 1

	def draw_fore(self, surface):
		surface.blit( self.tunnels[self.paths], self.rect.topleft)
		surface.blit( self.top, self.rect.topleft)
		return 0

	def affect_marble(self, board, marble, rpos):
		# Watch for marbles entering
		if (rpos[0]+marble_size == tile_size/2 and marble.direction == 1) or \
			(rpos[0]-marble_size == tile_size/2 and marble.direction == 3) or \
			(rpos[1]+marble_size == tile_size/2 and marble.direction == 2) or \
			(rpos[1]-marble_size == tile_size/2 and marble.direction == 0):

			if self.entering is not None:
				# Bump the marble that is currently entering
				newmarble = self.entering
				newmarble.rect.center = self.rect.center
				newmarble.direction = marble.direction

				play_sound( ping)

				# Let the base class affect the marble
				Tile.affect_marble(self, board, newmarble,
					(tile_size/2,tile_size/2))
			elif self.marble >= 0:
				# Bump the marble that is currently caught
				newmarble = Marble( self.marble, self.rect.center, marble.direction)

				board.marbles.append( newmarble)

				play_sound( ping)

				# Let the base class affect the marble
				Tile.affect_marble(self, board, newmarble,
					(tile_size/2,tile_size/2))

				self.marble = -1
				self.drawn = 0

			# Remember which marble is on its way in
			self.entering = marble

		elif rpos == (tile_size/2, tile_size/2):
			# Catch this marble
			self.marble = marble.color
			board.marbles.remove( marble)
			self.entering = None
			self.drawn = 0

class Painter(Tile):
	def __init__(self, paths, color, center=None):
		Tile.__init__(self, paths, center) # Call base class intializer
		self.color = color

	def draw_fore(self, surface):
		surface.blit( self.tunnels[self.paths], self.rect.topleft)
		surface.blit( self.images[self.color], self.rect.topleft)
		return 0

	def affect_marble(self, board, marble, rpos):
		Tile.affect_marble( self, board, marble, rpos)
		if rpos == (tile_size/2, tile_size/2):
			if marble.color != self.color:
				# Change the color
				marble.color = self.color
				play_sound( change_color)

class Filter(Tile):
	def __init__(self, paths, color, center=None):
		Tile.__init__(self, paths, center) # Call base class intializer
		self.color = color

	def draw_fore(self, surface):
		surface.blit( self.tunnels[self.paths], self.rect.topleft)
		surface.blit( self.images[self.color], self.rect.topleft)
		return 0

	def affect_marble(self, board, marble, rpos):
		if rpos == (tile_size/2, tile_size/2):
			# If the color is wrong, bounce the marble
			if marble.color != self.color and marble.color != 8:
				marble.direction = marble.direction ^ 2
				play_sound( ping)
			else:
				Tile.affect_marble( self, board, marble, rpos)
				play_sound( filter_admit)

class Director(Tile):
	def __init__(self, paths, direction, center=None):
		Tile.__init__(self, paths, center) # Call base class intializer
		self.direction = direction

	def draw_fore(self, surface):
		surface.blit( self.tunnels[self.paths], self.rect.topleft)
		surface.blit( self.images[self.direction], self.rect.topleft)
		return 0

	def affect_marble(self, board, marble, rpos):
		if rpos == (tile_size/2, tile_size/2):
			marble.direction = self.direction
			play_sound( direct_marble)

class Shredder(Tile):
	def __init__(self, paths, center=None):
		Tile.__init__(self, paths, center) # Call base class intializer

	def draw_fore(self, surface):
		surface.blit( self.tunnels[self.paths], self.rect.topleft)
		surface.blit( self.image, self.rect.topleft)
		return 0

	def affect_marble(self, board, marble, rpos):
		if rpos == (tile_size/2, tile_size/2):
			board.marbles.remove( marble)
			play_sound( shredder)

class Switch(Tile):
	def __init__(self, paths, dir1, dir2, center=None):
		Tile.__init__(self, paths, center) # Call base class intializer
		self.curdir = dir1
		self.otherdir = dir2
		self.switched = 0

	def switch(self):
		t = self.curdir
		self.curdir = self.otherdir
		self.otherdir = t
		self.switched = 1
		play_sound( switch)

	def draw_fore(self, surface):
		surface.blit( self.tunnels[self.paths], self.rect.topleft)
		surface.blit( self.images[self.curdir][self.otherdir],
			self.rect.topleft)
		rc = self.switched
		self.switched = 0
		return rc

	def affect_marble(self, board, marble, rpos):
		if rpos == (tile_size/2, tile_size/2):
			marble.direction = self.curdir
			self.switch()

class Replicator(Tile):
	def __init__(self, paths, count, center=None):
		Tile.__init__(self, paths, center) # Call base class intializer
		self.count = count
		self.pending = []

	def draw_fore(self, surface):
		surface.blit( self.tunnels[self.paths], self.rect.topleft)
		surface.blit( self.image, self.rect.topleft)
		return 0

	def update(self, board):
		for i in self.pending[:]:
			i[3] -= 1
			if i[3] == 0:
				i[3] = replicator_delay

				# Make sure that the active marble limit isn't exceeded
				if len(board.marbles) >= board.live_marbles_limit:
					# Clear the pending list
					self.pending = []
					return

				# Add the new marble
				board.marbles.append(Marble(i[0],self.rect.center,i[1]))
				play_sound( replicator)

				i[2] -= 1
				if i[2] <= 0: self.pending.remove( i)

	def affect_marble(self, board, marble, rpos):
		Tile.affect_marble( self, board, marble, rpos)
		if rpos == (tile_size/2, tile_size/2):
			# Add the marble to the pending list
			self.pending.append( [marble.color,marble.direction,
				self.count - 1, replicator_delay]);
			play_sound( replicator)

class Teleporter(Tile):
	def __init__(self, paths, other=None, center=None):
		Tile.__init__(self, paths, center) # Call base class intializer
		if paths & 5: self.image = self.image_v
		else: self.image = self.image_h
		if other is not None: self.connect( other)

	def draw_fore(self, surface):
		surface.blit( self.tunnels[self.paths], self.rect.topleft)
		surface.blit( self.image, self.rect.topleft)
		return 0

	def connect(self, other):
		self.other = other
		other.other = self

	def affect_marble(self, board, marble, rpos):
		if rpos == (tile_size/2, tile_size/2):
			marble.rect.center = self.other.rect.center
			play_sound( teleport)

class Trigger(Tile):
	def __init__(self, colors, center=None):
		Tile.__init__(self, 0, center) # Call base class intializer
		self.marbles = None
		self._setup( colors)

	def _setup(self, colors):
		self.countdown = 0
		self.marbles = [
			random.choice(colors),
			random.choice(colors),
			random.choice(colors),
			random.choice(colors),
			]
		self.drawn = 0

	def update(self, board):
		if self.countdown > 0:
			self.countdown -= 1
			if self.countdown == 0:
				self._setup( board.colors)
				play_sound( trigger_setup)

	def draw_back(self, surface):
		if self.drawn: return 0
		Tile.draw_back(self, surface)
		surface.blit( self.image, self.rect.topleft)
		if self.marbles is not None:
			for i in range(4):
				surface.blit( Marble.images[self.marbles[i]],
					(holecenters[0][i][0]+self.rect.left-marble_size/2,
					 holecenters[0][i][1]+self.rect.top-marble_size/2))
		return 1

	def complete(self, board):
		self.marbles = None
		self.countdown = trigger_time * frames_per_sec
		self.drawn = 0
		board.game.increase_score( 50)

class Stoplight(Tile):
	def __init__(self, colors, center=None):
		Tile.__init__(self, 0, center) # Call base class intializer
		self.marbles = list(colors)
		self.current = 0

	def draw_back(self, surface):
		if self.drawn: return 0
		Tile.draw_back(self, surface)
		surface.blit( self.image, self.rect.topleft)
		for i in range(self.current,3):
			surface.blit( self.smallmarbles[self.marbles[i]],
				(self.rect.centerx-14,
				 self.rect.top+3+(29*i)))
		return 1

	def complete(self, board):
		for i in range(3):
			if self.marbles[i] >= 0:
				self.marbles[i] = -1
				break
		self.current += 1
		self.drawn = 0
		board.game.increase_score( 20)

class Board:
	def __init__(self, game, pos):
		self.game = game
		self.pos = pos
		self.marbles = []
		self.screen = game.screen
		self.trigger = None
		self.stoplight = None
		self.launch_queue = []
		self.board_complete = 0
		self.paused = 0
		self.name = "Unnamed"
		self.live_marbles_limit = 10
		self.launch_timeout = -1
		self.board_timeout = -1
		self.colors = default_colors
		self.launched = 1

		self.set_launch_timer( default_launch_timer)
		self.set_board_timer( default_board_timer)

		# Create the board array
		self.tiles = []
		for j in range( vert_tiles):
			row = range( horiz_tiles)
			self.tiles.append( row)

		# Load the level
		# For levels above game.level, use a pseudo-random
		# level selection method.
		if( game.level < game.numlevels):
			self._load( game.circuit, game.level)
		else:
			# Compute a hash of the current level, involving
			# a static timestamp.  This provides a consistent,
			# backtrackable pseudo-random function.
			hash = md5.new(`game.gamestart`+"/"+`game.level`).digest()
			hashval = (ord(hash[0]) + (ord(hash[1]) << 8) + \
				(ord(hash[2]) << 16) + (ord(hash[3]) << 24)) & 32767;
			self._load( game.circuit, hashval % game.numlevels);

		# Create the launch timer text object
		self.launch_timer_text = launch_timer_font.render(
			`self.launch_timer`, 1, (255,255,255))
		self.launch_timer_text_rect = self.launch_timer_text.get_rect()
		self.launch_timer_text_rect.centerx = launch_timer_pos[0]+timer_width/2+1
		self.launch_timer_text_rect.bottom = \
			launch_timer_pos[1] + timer_height - timer_margin

		# Fill up the launch queue
		for i in range( vert_tiles * tile_size / marble_size + 2):
			self.launch_queue.append(random.choice(self.colors))

		# Create The Background
		self.background = pygame.Surface(screen.get_size()).convert()
		self.background.fill((200, 200, 200)) # Color of Info Bar

		# Draw the Backdrop
		backdrop = load_image('backdrop.jpg', None,
			(horiz_tiles * tile_size, vert_tiles * tile_size))
		self.background.blit( backdrop, board_pos);

		# Draw the launcher
		self.background.blit( self.launcher_background,
			(board_pos[0], board_pos[1] - marble_size))
		self.background.blit( self.launcher_v,
			(board_pos[0]+horiz_tiles*tile_size, board_pos[1]))
		for i in range( horiz_tiles):
			if self.tiles[0][i].paths & 1:
				self.background.blit( self.launcher_entrance, 
					(board_pos[0]+tile_size*i, board_pos[1]-marble_size))
		self.background.blit( self.launcher_corner,
			(board_pos[0]+horiz_tiles*tile_size-(tile_size-marble_size)/2,
			board_pos[1] - marble_size))

		# Draw the board name
		board_name = `self.game.level+1` + " - " + self.name
		if self.game.level >= self.game.numlevels:
			board_name += " (Random)"
		text = info_font.render( board_name, 1, (0,0,0))
		rect = text.get_rect()
		rect.left = 8
		self.background.blit( text, rect)

		# Figure out the score location
		text = "Score: 00000000"
		self.score_pos = screen_width - 8 - \
			info_font.render( text, 1, (0,0,0)).get_rect().width

		# Figure out the board timer location
		text = "00:00"
		self.board_timer_pos = self.score_pos - 16 - \
			info_font.render( text, 1, (0,0,0)).get_rect().width

		# Initialize the screen
		screen.blit(self.background, (0, 0))

	def draw_back(self, dirty_rects):
		# Draw the launch timer
		if self.launch_timer_height is None:
			height = timer_height
			rect = (launch_timer_pos[0],launch_timer_pos[1],
				timer_width,timer_height)
			self.screen.fill((0,0,0), rect)
			self.screen.fill((0,40,255),
				(launch_timer_pos[0]+timer_margin,
				launch_timer_pos[1]+timer_height-height,
				timer_width-timer_margin*2,height))
			dirty_rects.append( rect)
		else:
			height = timer_height*self.launch_timeout/self.launch_timeout_start
			if height < self.launch_timer_height:
				rect = (launch_timer_pos[0] + timer_margin,
					launch_timer_pos[1] + timer_height - self.launch_timer_height,
					timer_width-2*timer_margin, self.launch_timer_height - height)
				self.screen.fill((0,0,0), rect)
				dirty_rects.append( rect)
		self.launch_timer_height = height
		self.screen.blit( self.launch_timer_text, self.launch_timer_text_rect)
		dirty_rects.append( self.launch_timer_text_rect)

		# Clear the info bar
		rect = (0,0,screen_width,info_height)
		self.screen.set_clip( rect)
		self.screen.blit( self.background, (0,0))
		self.screen.set_clip()
		dirty_rects.append( rect)

		# Draw the score
		text = "Score: "+("00000000"+`self.game.score`)[-8:]
		text = info_font.render( text, 1, (0,0,0))
		rect = text.get_rect()
		rect.left = self.score_pos
		self.screen.blit( text, rect)

		# Draw the board timer
		time_remaining = (self.board_timeout+frames_per_sec-1)/frames_per_sec
		text = `time_remaining/60`+":"+("00"+`time_remaining%60`)[-2:]
		text = info_font.render( text, 1, (0,0,0))
		rect = text.get_rect()
		rect.left = self.board_timer_pos
		self.screen.blit( text, rect)

		# Draw the lives counter
		right_edge = self.board_timer_pos - 32
		for i in range(self.game.lives - 1):
			rect = self.life_marble.get_rect()
			rect.centery = info_height / 2
			rect.right = right_edge
			self.screen.blit( self.life_marble, rect)
			right_edge -= rect.width + 4

		# Draw the live marbles
		num_marbles = len(self.marbles)
		if num_marbles > self.live_marbles_limit:
			num_marbles = self.live_marbles_limit
		text = `num_marbles`+"/"+`self.live_marbles_limit`
		text = active_marbles_font.render( text, 1, (40,40,40))
		rect = text.get_rect()
		rect.left = self.pos[0] + 8
		rect.centery = self.pos[1] - marble_size / 2
		rect.width += 100
		self.screen.set_clip( rect)
		self.screen.blit( self.background, (0,0))
		self.screen.set_clip()
		self.screen.blit( text, rect)

		dirty_rects.append( rect)

		for row in self.tiles:
			for tile in row:
				if tile.draw_back( self.background):
					self.screen.set_clip( tile.rect)
					self.screen.blit( self.background, (0,0))
					self.screen.set_clip()
					dirty_rects.append( tile.rect)

		if self.launched:
			for i in range(len(self.launch_queue)):
				self.background.blit( Marble.images[self.launch_queue[i]],
					(self.pos[0] + horiz_tiles * tile_size,
					self.pos[1] + i * marble_size - marble_size))
			rect = (self.pos[0] + horiz_tiles * tile_size,
					self.pos[1] - marble_size, marble_size,
					marble_size + tile_size * vert_tiles)
			self.screen.set_clip( rect)
			self.screen.blit( self.background, (0,0))
			self.screen.set_clip()
			dirty_rects.append( rect)
			self.launched = 0

	def draw_fore(self, dirty_rects):
		for row in self.tiles:
			for tile in row:
				if tile.draw_fore(self.screen):
					dirty_rects.append( tile.rect)

	def update(self):
		# Create the list of dirty rectangles
		dirty_rects = []

		# Erase the marbles
		for marble in self.marbles:
			marble.undraw( self.screen, self.background)
			dirty_rects.append( list(marble.rect))

		# Animate the marbles
		for marble in self.marbles[:]:
			marble.update( self)

		# Animate the tiles
		for row in self.tiles:
			for tile in row:
				tile.update( self)
				if tile.drawn == 0: dirty_rects.append( tile.rect)

		# Complete any wheels, if appropriate
		try_again = 1
		while try_again:
			try_again = 0
			for row in self.tiles:
				for tile in row:
					if isinstance( tile, Wheel):
						try_again |= tile.maybe_complete( self)

		# Check if the board is complete
		self.board_complete = 1
		for row in self.tiles:
			for tile in row:
				if isinstance( tile, Wheel):
					if tile.completed == 0: self.board_complete = 0

		# Decrement the launch timer
		if self.launch_timeout > 0:
			self.launch_timeout -= 1
			if self.launch_timeout == 0: self.board_complete = -1

		# Decrement the board timer
		if self.board_timeout > 0:
			self.board_timeout -= 1
			if self.board_timeout == 0: self.board_complete = -2

		# Draw the background
		self.draw_back( dirty_rects)

		# Draw all of the marbles
		for marble in self.marbles:
			marble.draw( self.screen)
			dirty_rects.append( marble.rect)

		# Draw the foreground
		self.draw_fore( dirty_rects)

		# Flip the display
		pygame.display.update( dirty_rects)

	def set_tile(self, x, y, tile):
		self.tiles[y][x] = tile
		tile.rect.left = self.pos[0] + tile_size * x
		tile.rect.top = self.pos[1] + tile_size * y

		tile.x = x
		tile.y = y

		# If it's a trigger, keep track of it
		if isinstance( tile, Trigger):
			self.trigger = tile

		# If it's a stoplight, keep track of it
		if isinstance( tile, Stoplight):
			self.stoplight = tile

	def set_launch_timer(self, passes):
		self.launch_timer = passes
		self.launch_timeout_start = (marble_size +
			(horiz_tiles * tile_size - marble_size) * passes) / marble_speed
		self.launch_timer_height = None

	def set_board_timer(self, seconds):
		self.board_timer = seconds
		self.board_timeout_start = seconds * frames_per_sec
		self.board_timeout = self.board_timeout_start

	def launch_marble(self):
		self.launch_queue.append(random.choice(self.colors))
		self.marbles.insert( 0, Marble( self.launch_queue[0],
			(self.pos[0]+tile_size*horiz_tiles+marble_size/2,
			self.pos[1]-marble_size/2), 3))
		del self.launch_queue[0]
		self.launched = 1

		self.launch_timeout = self.launch_timeout_start
		self.launch_timer_height = None

	def affect_marble(self, marble):
		c = marble.rect.center
		cx = c[0] - self.pos[0]
		cy = c[1] - self.pos[1]

		# Bounce marbles off of the top
		if cy == marble_size/2:
			marble.direction = 2
			return

		if cy < 0:
			if cx == marble_size/2:
				marble.direction = 1
				return
			if cx == tile_size * horiz_tiles - marble_size/2 \
				and marble.direction == 1:
				marble.direction = 3
				return

			# The special case of new marbles at the top
			effective_cx = cx
			effective_cy = cy + marble_size
		else:
			effective_cx = cx + marble_size/2 * dirs[marble.direction][0]
			effective_cy = cy + marble_size/2 * dirs[marble.direction][1]

		tile_x = effective_cx / tile_size
		tile_y = effective_cy / tile_size
		tile_xr = cx - tile_x * tile_size
		tile_yr = cy - tile_y * tile_size

		if tile_x >= horiz_tiles: return

		tile = self.tiles[tile_y][tile_x]

		if cy < 0 and marble.direction != 2:
			# The special case of new marbles at the top
			if tile_xr == tile_size / 2 and (tile.paths & 1):
				if isinstance( tile, Wheel):
					if tile.spinpos > 0 or tile.marbles[0] != -3: return
					tile.marbles[0] = -2
					marble.direction = 2
					self.launch_marble()
				elif len(self.marbles) < self.live_marbles_limit:
					marble.direction = 2
					self.launch_marble()
		else:
			tile.affect_marble( self, marble, (tile_xr, tile_yr))

	def click(self, pos):
		# Determine which tile the pointer is in
		tile_x = (pos[0] - self.pos[0]) / tile_size
		tile_y = (pos[1] - self.pos[1]) / tile_size
		tile_xr = pos[0] - self.pos[0] - tile_x * tile_size
		tile_yr = pos[1] - self.pos[1] - tile_y * tile_size
		if tile_x >= 0 and tile_x < horiz_tiles and \
			tile_y >= 0 and tile_y < vert_tiles:
			tile = self.tiles[tile_y][tile_x]
			tile.click( self, tile_xr, tile_yr, tile_x, tile_y)

	def _load(self, circuit, level):
		fullname = os.path.join('circuits', circuit)
		f = open( fullname)

		# Skip the previous levels
		j = 0
		while j < vert_tiles * level:
			line = f.readline()
			if line == '':
				f.close()
				return 0
			if line[0] == '|': j += 1

		teleporters = []
		teleporter_names = []
		stoplight = default_stoplight

		numwheels = 0
		boardtimer = -1

		j = 0
		while j < vert_tiles:
			line = f.readline()

			if line[0] != '|':
				if line[0:5] == 'name=':
					self.name = line[5:-1]
				elif line[0:11] == 'maxmarbles=':
					self.live_marbles_limit = int(line[11:-1])
				elif line[0:12] == 'launchtimer=':
					self.set_launch_timer( int(line[12:-1]))
				elif line[0:11] == 'boardtimer=':
					boardtimer = int(line[11:-1])
				elif line[0:7] == 'colors=':
					self.colors = []
					for c in line[7:-1]:
						if c >= '0' and c <= '7':
							self.colors.append(int(c))
							self.colors.append(int(c))
							self.colors.append(int(c))
						elif c == '8':
							# Crazy marbles are one-third as common
							self.colors.append(8)
				elif line[0:10] == 'stoplight=':
					stoplight = []
					for c in line[10:-1]:
						if c >= '0' and c <= '7':
							stoplight.append(int(c))

				continue

			for i in range(horiz_tiles):
				type = line[i*4+1]
				paths = line[i*4+2]
				if paths == ' ': pathsint = 0
				elif paths >= 'a': pathsint = ord(paths)-ord('a')+10
				elif paths >= '0' and paths <= '9': pathsint = int(paths)
				else: pathsint = int(paths)
				color = line[i*4+3]
				if color == ' ': colorint = 0
				elif color >= 'a': colorint = ord(color)-ord('a')+10
				elif color >= '0' and color <= '9': colorint = int(color)
				else: colorint = 0

				if type == 'O':
					tile = Wheel( pathsint)
					numwheels += 1
				elif type == '%': tile = Trigger(self.colors)
				elif type == '!': tile = Stoplight(stoplight)
				elif type == '&': tile = Painter(pathsint, colorint)
				elif type == '#': tile = Filter(pathsint, colorint)
				elif type == '@':
					if color == ' ': tile = Buffer(pathsint)
					else: tile = Buffer(pathsint, colorint)
				elif type == ' ' or \
					(type >= '0' and type <= '8'): tile = Tile(pathsint)
				elif type == 'X': tile = Shredder(pathsint)
				elif type == '*': tile = Replicator(pathsint, colorint)
				elif type == '^':
					if color == ' ': tile = Director(pathsint, 0)
					elif color == '>': tile = Switch(pathsint, 0, 1)
					elif color == 'v': tile = Switch(pathsint, 0, 2)
					elif color == '<': tile = Switch(pathsint, 0, 3)
				elif type == '>':
					if color == ' ': tile = Director(pathsint, 1)
					elif color == '^': tile = Switch(pathsint, 1, 0)
					elif color == 'v': tile = Switch(pathsint, 1, 2)
					elif color == '<': tile = Switch(pathsint, 1, 3)
				elif type == 'v':
					if color == ' ': tile = Director(pathsint, 2)
					elif color == '^': tile = Switch(pathsint, 2, 0)
					elif color == '>': tile = Switch(pathsint, 2, 1)
					elif color == '<': tile = Switch(pathsint, 2, 3)
				elif type == '<':
					if color == ' ': tile = Director(pathsint, 3)
					elif color == '^': tile = Switch(pathsint, 3, 0)
					elif color == '>': tile = Switch(pathsint, 3, 1)
					elif color == 'v': tile = Switch(pathsint, 3, 2)
				elif type == '=':
					if color in teleporter_names:
						other = teleporters[teleporter_names.index(color)]
						tile = Teleporter( pathsint, other)
					else:
						tile = Teleporter( pathsint)
						teleporters.append( tile)
						teleporter_names.append( color)

				self.set_tile( i, j, tile)

				if type >= '0' and type <= '8':
					if color == '^': direction = 0
					elif color == '>': direction = 1
					elif color == 'v': direction = 2
					else: direction = 3
					self.marbles.append(
						Marble(int(type),tile.rect.center,direction))

			j += 1
		if boardtimer < 0: boardtimer = default_board_timer * numwheels
		self.set_board_timer( boardtimer)
		f.close()
		return 1

	# Return values for this function:
	# -4: User closed the application window
	# -3: User aborted the level
	# -2: Board timer expired
	# -1: Launch timer expired
	#  1: Level completed successfully
	#  2: User requested a skip to the next level
	#  3: User requested a skip to the previous level
	def play_level( self):
		# Perform the first render
		self.update()

		# Play the start sound
		#play_sound( levelbegin)

		# Launch the first marble
		self.launch_marble()

		# Do the first update
		pygame.display.update()

		# Game Loop
		while not self.board_complete:
			# Wait for the next frame
			my_tick( frames_per_sec)

			# Handle Input Events
			for event in pygame.event.get():
				if event.type is QUIT:
					return -4
				elif event.type is KEYDOWN:
					if event.key is K_ESCAPE: return -3
					elif event.key == ord('n'): return 2
					elif event.key == ord('b'): return 3
					elif event.key == ord(' ') or \
						event.key == ord('p') or \
						event.key == K_PAUSE:
						self.paused = self.paused ^ 1
						if self.paused:
							if screenshot:
								pause_popup = None
							else:
								pause_popup = popup('Game Paused')
						else:
							popdown( pause_popup)
					elif event.key == K_F2:
						toggle_fullscreen()
					elif event.key == K_F3:
						toggle_music()
					elif event.key == K_F4:
						toggle_sound()

				elif event.type is MOUSEBUTTONDOWN:
					if self.paused:
						self.paused = 0
						popdown( pause_popup)
					else: self.click( pygame.mouse.get_pos())

			if not self.paused: self.update()

		# Play the end sound
		if self.board_complete > 0:
			play_sound( levelfinish)
		else:
			play_sound( die)

		return self.board_complete

class HighScores:
	num_highscores = 10

	def __init__(self, filename):
		self.filename = filename
		self.current_score = -1
		self.load()

	def qualifies(self, score):
		self.load()
		return score >= self.scores[-1][0]

	def add_score(self, score, circuit, level, name):
		self.load()
		for i in range(len(self.scores)):
			if score >= self.scores[i][0]:
				self.scores.insert( i, (score, circuit, level, name))
				del self.scores[self.num_highscores:]
				self.save()
				self.current_score = i
				return i
		return -1

	def load( self):
		self.scores = []

		parser = re.compile("([0-9]+) ([^ ]+) ([0-9]+) (.*)\n")

		try:
			f = open( self.filename)
			while len(self.scores) < self.num_highscores:
				line = f.readline()
				if line == '': break
				match = parser.match(line)
				if match is not None:
					(score,circuit,level,name) = match.groups()
					self.scores.append(
						(int(score), circuit, int(level), name))
			f.close()
		except: pass

		# Extend the list if it is shorter than needed
		while len(self.scores) < self.num_highscores:
			self.scores.append( (0, 'all-boards', 1, ''))

		# Shrink the list if it is longer than allowed
		del self.scores[self.num_highscores:]

	def save(self):
		try:
			f = open( self.filename, "w")
		except:
			try:
				f = os.popen(write_highscores, "w")
			except OSError, message:
				print "Warning: Can't save highscores:", message
				return

		try:
			for i in self.scores:
				f.write( `i[0]`+' '+i[1]+' '+`i[2]`+' '+i[3]+'\n')
			f.close()
		except:
			print "Warning: Problem saving highscores."

def wait_one_sec():
	time.sleep(1)
	pygame.event.get() # Clear the event queue

def popup( text, minsize=None):
	maxwidth = 0
	objs = []
	while text != "":
		if '\n' in text:
			newline = text.index('\n')
			line = text[:newline]
			text = text[newline+1:]
		else:
			line = text
			text = ""

		obj = popup_font.render( line, 1, (0, 0, 0))
		maxwidth = max( maxwidth, obj.get_rect().width)
		objs.append( obj)

	linespacing = popup_font.get_ascent() - \
		popup_font.get_descent() + popup_font.get_linesize()
	# Work around an apparent pygame bug on Windows
	linespacing = min( linespacing, int(1.2 * popup_font.get_height()))

	# Leave a bit more room
	linespacing = int(linespacing * 1.3)

	window_width = maxwidth + 40
	window_height = popup_font.get_height()+linespacing*(len(objs)-1)+40
	if minsize is not None:
		window_width = max( window_width, minsize[0])
		window_height = max( window_height, minsize[1])

	window = pygame.Surface((window_width, window_height))
	winrect = window.get_rect()
	window.fill((0, 0, 0))
	window.fill((250, 250, 250), winrect.inflate(-2,-2))

	y = 20
	for obj in objs:
		textpos = obj.get_rect()
		textpos.top = y
		textpos.centerx = winrect.centerx
		window.blit( obj, textpos)
		y += linespacing

	winrect.center = screen.get_rect().center
	winrect.top -= 40

	backbuf = pygame.Surface(winrect.size).convert()
	backbuf.blit( screen, (0,0), winrect)

	screen.blit( window, winrect)
	pygame.display.update()

	return (backbuf, winrect)

def popdown( popup_rc):
	if popup_rc is not None:
		screen.blit( popup_rc[0], popup_rc[1])
		pygame.display.update( popup_rc[1])

class Game:
	def __init__(self, screen, circuit, highscores):
		self.screen = screen
		self.circuit = circuit
		self.highscores = highscores

		# Count the number of levels
		fullname = os.path.join('circuits', circuit)
		f = open( fullname)
		j = 0
		while 1:
			line = f.readline()
			if line == '': break
			if line[0] == '|': j += 1
		f.close()
		self.numlevels = j / vert_tiles

		self.level = 0
		self.score = 0
		self.lives = initial_lives

		self.gamestart = time.time()

	def increase_score(self, amount):
		# Add the amount to the score
		self.score += amount

		# Award any extra lives that are due
		extra_lives = amount / extra_life_frequency + \
			(self.score % extra_life_frequency < amount % extra_life_frequency)
		extra_lives = min( extra_lives, max_spare_lives+1 - self.lives)
		if extra_lives > 0:
			self.lives += extra_lives
			play_sound( extra_life)

	# Return values for this function:
	# -1: User closed the application window
	#  0: The game was aborted
	#  1: Game completed normally
	#  2: User achieved a highscore
	def play(self):
		# Draw the loading screen
		backdrop = load_image('backdrop.jpg', None,
			(screen_width, screen_height))
		screen.blit( backdrop, (0,0))
		pygame.display.update()

		popup("Please wait...\n", (150, 50))

		start_music("background.xm", ingame_music_volume)

		self.highscores.current_score = -1

		while 1:
			# Play a level
			board = Board( self, board_pos)

			rc = board.play_level()

			# Check for the user closing the window
			if rc == -4: return -1

			if rc == 2:
				self.level += 1
				continue

			if rc == 3:
				if self.level > 0: self.level -= 1
				self.score = 0
				self.lives = initial_lives
				continue

			if rc < 0:
				# The board was not completed

				if rc == -3: message = 'Level Aborted.'
				elif rc == -2: message = 'The board timer has expired.'
				else: message = 'The launch timer has expired.'

				self.lives -= 1
				if self.lives > 0:
					rc = self.board_dialog( message+'\nClick to try again.',
						rc != -3)
				elif self.highscores.qualifies( self.score):
					popup("Congratulations!\n"+
						"You have a highscore!\n"+
						"Please enter your name:", (300, 180))
					name = get_name( self.screen, popup_font,
						((screen_width-250)/2,310,250,popup_font.get_height()),
						(255,255,255), (0,0,0))
					if name is None: return -1

					self.highscores.add_score( self.score, self.circuit,
						self.level+1, name)

					return 2
				else:
					rc = self.board_dialog( message +
						'\nGame Over.\nClick to continue\n', rc != -3)
					if rc == 1: return 1

					self.score = 0
					self.lives = initial_lives
			else:
				# The board was completed

				# Compute time remaining bonus
				time_remaining = 100 * board.board_timeout / \
					board.board_timeout_start
				time_bonus = 5 * time_remaining

				# Compute empty holes bonus
				total_holes = 0
				empty_holes = 0
				for row in board.tiles:
					for tile in row:
						if isinstance( tile, Wheel):
							total_holes += 4
							for i in tile.marbles:
								if i < 0: empty_holes += 1
				empty_holes = (100 * empty_holes + total_holes/2) / total_holes
				holes_bonus = 2 * empty_holes

				self.increase_score( time_bonus + holes_bonus)

				message = 'Level Complete!\n'+ \
					"Bonus for " + `time_remaining` + "% time remaining: " + \
					`time_bonus` + "\n" + \
					"Bonus for " + `empty_holes` + "% holes empty: " + \
					`holes_bonus` + '\nClick to continue.'

				rc = self.board_dialog( message, 1, 1)
				self.level += 1

			if rc == -2: return -1
			if rc < 0: return 0

	# Return values for this function:
	# -2: User closed the application window
	# -1: User pressed escape
	#  0: User pressed 'b' or 'n'
	#  1: Some other user event
	def board_dialog( self, message, pause=1, complete=0):
		popup(message)
		if pause: wait_one_sec()

		# Wait for a mouse click to continue
		while 1:
			pygame.time.wait(20)
			for event in pygame.event.get():
				if event.type is QUIT:
					return -2
				elif event.type is KEYDOWN:
					if event.key == K_ESCAPE: return -1
					if event.key == ord('b'):
						if self.level > 0: self.level -= 1
						self.score = 0
						self.lives = initial_lives
						return 0
					elif event.key == ord('n'):
						if complete == 0:
							# Skip to the next level
							self.level += 1
						return 0
					elif event.key == K_F2:
						toggle_fullscreen()
						continue
					elif event.key == K_F3:
						toggle_music()
						continue
					elif event.key == K_F4:
						toggle_sound()
						continue
					elif event.key == K_LSHIFT or \
						event.key == K_RSHIFT or \
						event.key == K_LALT or \
						event.key == K_RALT or \
						event.key == K_LCTRL or \
						event.key == K_RCTRL:
						continue
					return 1
				elif event.type is MOUSEBUTTONDOWN:
					return 1

def translate_key( key, shift_state):
	if shift_state:
		if key >= ord('a') and key <= ord('z'): key += ord('A') - ord('a')
		elif key == ord('1'): key = ord('!')
		elif key == ord('2'): key = ord('@')
		elif key == ord('3'): key = ord('#')
		elif key == ord('4'): key = ord('$')
		elif key == ord('5'): key = ord('%')
		elif key == ord('6'): key = ord('^')
		elif key == ord('7'): key = ord('&')
		elif key == ord('8'): key = ord('*')
		elif key == ord('9'): key = ord('(')
		elif key == ord('0'): key = ord(')')
		elif key == ord('`'): key = ord('~')
		elif key == ord("'"): key = ord('"')
		elif key == ord(";"): key = ord(':')
		elif key == ord("\\"): key = ord('|')
		elif key == ord("["): key = ord('{')
		elif key == ord("]"): key = ord('}')
		elif key == ord(","): key = ord('<')
		elif key == ord("."): key = ord('>')
		elif key == ord("/"): key = ord('?')
		elif key == ord("-"): key = ord('_')
		elif key == ord("="): key = ord('+')
	return key

def get_name( screen, font, cursor_box, backcol, forecol):
	cursor_width = cursor_box[3] / 3
	cursor_pos = [cursor_box[0], cursor_box[1], cursor_width, cursor_box[3]]
	name = ""

	inner_box = pygame.Rect(cursor_box)
	cursor_box = inner_box.inflate( 2, 2)
	outer_box = cursor_box.inflate( 2, 2)

	enter_pressed = 0
	while not enter_pressed:
		screen.fill( forecol, outer_box)
		screen.fill( backcol, cursor_box)
		cursor_pos[0] = inner_box.left
		if name != "":
			obj = font.render( name, 1, forecol)
			screen.blit( obj, inner_box)
			cursor_pos[0] += obj.get_width()
		screen.fill( forecol, cursor_pos)
		pygame.display.update( (outer_box,))

		# Keep track of the shift keys
		shift_state = pygame.key.get_mods() & KMOD_SHIFT

		pygame.time.wait(20)
		for event in pygame.event.get():
			if event.type is QUIT:
				return None
			elif event.type is KEYUP:
				if event.key == K_LSHIFT:
					shift_state &= ~KMOD_LSHIFT
				elif event.key == K_RSHIFT:
					shift_state &= ~KMOD_RSHIFT
			elif event.type is KEYDOWN:
				if event.key == K_LSHIFT:
					shift_state |= KMOD_LSHIFT
				elif event.key == K_RSHIFT:
					shift_state |= KMOD_RSHIFT
				elif event.key == K_ESCAPE or event.key == K_RETURN:
					enter_pressed = 1
					break
				elif event.key == K_F2:
					toggle_fullscreen()
				elif event.key == K_F3:
					toggle_music()
				elif event.key == K_F4:
					toggle_sound()
				elif event.key == K_BACKSPACE:
					name = name[:-1]
				elif event.key >= 32 and event.key <= 127:
					key = translate_key( event.key, shift_state)
					name = name + chr(key)

	return name

class IntroScreen:
	menu = ("Start Game", "High Scores", "Fullscreen:", "Music:",
		"Sound Effects:", "Quit Game")
	menu_width = 240
	menu_pos = ((800 - menu_width)/2, 145)
	menu_font_height = 32
	menu_color = (255,255,255)
	menu_cursor_color = (60,60,60)
	menu_cursor_leftright_margin = 2
	menu_cursor_bottom_margin = -2
	menu_cursor_top_margin = 0
	menu_option_left = 200
	menu_rect = (menu_pos[0]-menu_cursor_leftright_margin,
		 menu_pos[1]-menu_cursor_top_margin,
		 menu_width + 2 * menu_cursor_leftright_margin,
		 menu_font_height * len(menu) +
			menu_cursor_top_margin + menu_cursor_bottom_margin)

	scroller_font_height = 28
	scroller_rect = (10,550,780,scroller_font_height)
	scroller_text = \
		"   Copyright © 2003  John-Paul Gignac.   "+ \
		"   Soundtrack by Matthias Le Bidan.   "+ \
		"   Board designs contributed by Mike Brenneman and Kim Gignac.   "+ \
		"   To contribute your own board designs, see the website:  "+ \
		"http://pathological.sourceforge.net/   "+ \
		"   Logo by Carrie Bloomfield.   "+ \
		"   Other graphics based on artwork by Mike Brenneman.   "+ \
		"   Project motivated by Paul Prescod.   "+ \
		"   Thanks to all my friends who helped make this project "+ \
		"a success!   "+ \
		"   This program is free software; you can redistribute it and/or "+ \
		"modify it under the terms of the GNU General Public License.  "+ \
		"See the LICENSE file for details.   "

	scroller_color = (60,60,60)
	scroller_speed = 2

	def __init__(self, screen, highscores):
		self.screen = screen
		self.highscores = highscores
		self.curpage = 0

		self.scroller_image = self.scroller_font.render(
			self.scroller_text, 1, self.scroller_color)

		self.menu_cursor = 0

	def draw_background(self):
		self.screen.blit( self.background, (0,0))

	def undraw_menu(self):
		if self.curpage == 1:
			self.undraw_highscores()
			return

		self.screen.set_clip( self.menu_rect)
		self.draw_background()
		self.screen.set_clip()
		self.dirty_rects.append( self.menu_rect)

	def undraw_highscores(self):
		self.screen.set_clip( self.hs_rect)
		self.draw_background()
		self.screen.set_clip()
		self.dirty_rects.append( self.hs_rect)

	def draw_menu(self):
		if self.curpage == 1:
			self.draw_highscores()
			return

		self.undraw_menu()

		self.screen.fill( self.menu_cursor_color,
			(self.menu_pos[0]-self.menu_cursor_leftright_margin,
			 self.menu_pos[1]-self.menu_cursor_top_margin +
			 self.menu_cursor * self.menu_font_height,
			 self.menu_width + 2 * self.menu_cursor_leftright_margin,
			 self.menu_font_height + self.menu_cursor_top_margin +
			 self.menu_cursor_bottom_margin))

		y = self.menu_pos[1]
		for i in self.menu:
			menu_option = self.menu_font.render(i, 1, self.menu_color)
			self.screen.blit( menu_option, (self.menu_pos[0], y))
			y += self.menu_font_height

		if fullscreen: offon = 'On'
		else: offon = 'Off'
		offon = self.menu_font.render( offon, 1, self.menu_color)
		self.screen.blit( offon,
			(self.menu_pos[0]+self.menu_option_left,
			self.menu_pos[1]+self.menu_font_height * 2))

		if music_on: offon = 'On'
		else: offon = 'Off'
		offon = self.menu_font.render( offon, 1, self.menu_color)
		self.screen.blit( offon,
			(self.menu_pos[0]+self.menu_option_left,
			self.menu_pos[1]+self.menu_font_height * 3))

		if sound_on: offon = 'On'
		else: offon = 'Off'
		offon = self.menu_font.render( offon, 1, self.menu_color)
		self.screen.blit( offon,
			(self.menu_pos[0]+self.menu_option_left,
			self.menu_pos[1]+self.menu_font_height * 4))

		self.dirty_rects.append( self.menu_rect)

	def draw_scroller(self):
		self.screen.set_clip( self.scroller_rect)
		self.draw_background()
		self.screen.blit( self.scroller_image,
			(self.scroller_rect[0] - self.scroller_pos,
			self.scroller_rect[1]))
		self.screen.set_clip()
		self.dirty_rects.append( self.scroller_rect)

	def draw(self):
		self.dirty_rects = []
		self.draw_background()
		self.draw_menu()
		self.draw_scroller()
		pygame.display.update()

	def go_to_main_menu(self):
		# Return to the main menu
		play_sound( menu_select)
		self.undraw_menu()
		self.curpage = 0
		self.draw_menu()

	def go_to_highscores(self):
		# Go to the highscores page
		self.undraw_menu()
		self.curpage = 1
		self.draw_menu()

	def do(self, show_highscores=0):
		self.scroller_pos = -self.scroller_rect[2]

		if( show_highscores):
			self.dirty_rects = []
			self.go_to_highscores()
			pygame.display.update( self.dirty_rects)

		self.draw()

		start_music("intro.xm", intro_music_volume)

		while 1:
			# Wait for the next frame
			my_tick( frames_per_sec)

			self.dirty_rects = []
			self.draw_scroller()

			# Advance the scroller
			self.scroller_pos += self.scroller_speed
			if self.scroller_pos >= self.scroller_image.get_rect().width:
				self.scroller_pos = -self.scroller_rect[2]

			pygame.time.wait(20)
			for event in pygame.event.get():
				if event.type is QUIT:
					if self.curpage == 1:
						self.go_to_main_menu()
						continue
					return -2
				elif event.type is KEYDOWN:
					if event.key == K_F2:
						play_sound( menu_select)
						if not toggle_fullscreen(): return -3
						self.draw_menu()
					elif event.key == K_F3:
						play_sound( menu_select)
						toggle_music()
						self.draw_menu()
					elif event.key == K_F4:
						toggle_sound(1, self.dirty_rects)
						play_sound( menu_select)
						self.draw_menu()
					elif self.curpage == 1:
						self.go_to_main_menu()
					elif event.key == K_ESCAPE:
						return -1
					elif event.key == K_DOWN:
						self.menu_cursor += 1
						play_sound( menu_scroll)
						if self.menu_cursor == len(self.menu):
							self.menu_cursor = 0
						self.draw_menu()
					elif event.key == K_UP:
						self.menu_cursor -= 1
						play_sound( menu_scroll)
						if self.menu_cursor < 0:
							self.menu_cursor = len(self.menu) - 1
						self.draw_menu()
					elif event.key == K_SPACE or event.key == K_RETURN:
						rc = self.menu_select( self.menu_cursor)
						if rc < 1: return rc
					continue
				elif event.type is MOUSEBUTTONDOWN:
					if self.curpage == 1:
						self.go_to_main_menu()
						continue

					pos = pygame.mouse.get_pos()

					# Figure out which menu option is being clicked, if any

					if pos[0] < self.menu_pos[0]: continue
					if pos[0] >= self.menu_pos[0] + self.menu_width: continue
					if pos[1] < self.menu_pos[1]: continue
					i = (pos[1] - self.menu_pos[1]) / self.menu_font_height
					if i >= len(self.menu): continue

					rc = self.menu_select( i)
					if rc < 1: return rc

			pygame.display.update( self.dirty_rects)

	# Return values:
	# -3 - Toggle fullscreen requires warm restart
	# -1 - User selected the Quit option
	#  0 - User selected Begin Game
	#  1 - Unknown option
	def menu_select( self, i):
		if i == 0:
			return 0
		elif i == 1:
			play_sound( menu_select)
			self.go_to_highscores()
		elif i == 2:
			play_sound( menu_select)
			if not toggle_fullscreen(): return -3
			self.draw_menu()
		elif i == 3:
			play_sound( menu_select)
			toggle_music()
			self.draw_menu()
		elif i == 4:
			toggle_sound()
			play_sound( menu_select)
			self.draw_menu()
		elif i == 5:
			return -1
		return 1

	hs_font_height = 24
	hs_width = 320
	hs_pos = ((800-hs_width)/2, 114)
	hs_margin = 8
	hs_column_margin = 70
	hs_score_width = 70
	hs_level_width = 24
	hs_heading_color = (0,0,0)
	hs_heading_background = (0,80,0)
	hs_number_color = (210,210,210)
	hs_body_color = (240,240,240)
	hs_current_color = (240,50,50)
	hs_rows = HighScores.num_highscores
	hs_rect = (hs_pos[0],hs_pos[1],hs_width,hs_font_height * 11)

	def draw_highscores(self):
		self.undraw_menu()

		tname = self.hs_font.render("Name", 1, self.hs_heading_color)
		tscore = self.hs_font.render("Score", 1, self.hs_heading_color)
		tlevel = self.hs_font.render("Level", 1, self.hs_heading_color)

		# Compute the column positions
		score_right = self.hs_width
		score_left = score_right - tscore.get_size()[0]
		level_right = score_right - self.hs_score_width - \
			self.hs_margin
		level_left = level_right - tlevel.get_size()[0]
		name_right = level_right - self.hs_level_width - \
			self.hs_margin
		number = self.hs_font.render('10.',1,(0,0,0))
		number_width = number.get_size()[0]
		name_left = number_width + 5
		name_width = name_right - name_left

		x = self.hs_pos[0]
		for j in range(len(self.highscores.scores)):
			if (j % self.hs_rows) == 0:
				if j > 0: x += self.hs_column_margin + self.hs_width
				y = self.hs_pos[1]

				# Draw the headings
#				self.screen.fill( self.hs_heading_background,
#					(x - 30, y, self.hs_width + 30, self.hs_font_height))
				self.screen.blit( tname, (x + name_left, y))
				self.screen.blit( tlevel, (x + level_left, y))
				self.screen.blit( tscore, (x + score_left, y))

			i = self.highscores.scores[j]

			numcolor = self.hs_number_color
			color = self.hs_body_color
			if j == self.highscores.current_score:
				numcolor = color = self.hs_current_color

			y += self.hs_font_height
			number = self.hs_font.render(`j+1`+'.',1,numcolor)
			self.screen.blit( number,
				(x + number_width - number.get_size()[0], y))
			if i[3] != '':
				name = self.hs_font.render( i[3], 1, color)
				if name.get_width() > name_width:
					name = name.subsurface( (0,0,name_width,name.get_height()))
				self.screen.blit( name, (x + name_left, y))
			level = self.hs_font.render( `i[2]`, 1, color)
			self.screen.blit( level, (x + level_right - level.get_width(), y))
			score = self.hs_font.render( `i[0]`, 1, color)
			self.screen.blit( score, (x + score_right - score.get_width(), y))

		self.dirty_rects.append( self.hs_rect)

def setup_everything():
	global introscreen

	# Configure the audio settings
	if sys.platform[0:3] == 'win':
		# On Windows platforms, increase the sample rate and the buffer size
		pygame.mixer.pre_init(44100,-16,1,4096)

	# Initialize the game module
	pygame.init()

	if not pygame.font: print 'Warning, fonts disabled'
	if not pygame.mixer: print 'Warning, sound disabled'

	set_video_mode()
	load_sounds()
	load_fonts()
	load_images()

	introscreen = IntroScreen( screen, highscores)

# Load the highscores file
highscores = HighScores( highscores_file)

setup_everything()

# Main loop
show_highscores = 0
while 1:
	# Display the intro screen
	while 1:
		rc = introscreen.do( show_highscores)
		if rc == -3:
			# Warm restart to toggle fullscreen
			fullscreen = fullscreen ^ 1
			setup_everything()
		else:
			break

	if rc < 0: break   # Handle the QUIT message

	game = Game(screen, 'all-boards', highscores)

	show_highscores = 1

	rc = game.play()
	if rc < 0: break   # Handle the QUIT message
	if rc == 0: show_highscores = 0


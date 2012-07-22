package org.gignac.jp.pathological;

class Trigger extends Tile {
	def __init__(self, colors, center=None):
		Tile.__init__(self, 0, center) // Call base class intializer
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
}

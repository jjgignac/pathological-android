package org.gignac.jp.pathological;

class Stoplight extends Tile {
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
}

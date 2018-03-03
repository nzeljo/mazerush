rle = "R30D78L12"

parsepos = 0
destination = 84
cmove = 0
movecounter = 0

while(cmove < destination and parsepos < len(rle)):
	while(movecounter == 0):
		cchar = rle[parsepos]
		if(cchar.isalpha()):
			cdir = cchar
			parsepos+=1
			cnum = 0
		elif(cchar.isdigit()):
			while(rle[parsepos].isdigit() and parsepos < len(rle)):
				cnum = cnum * 10
				cnum += int(rle[parsepos])
				parsepos+= 1
				if parsepos >= len(rle):
				    break
			movecounter = cnum
		else:
			cdir = "x"
			break
	cmove+=1
	movecounter -= 1
if destination != cmove:
    cdir = "x"
print cdir

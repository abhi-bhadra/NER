# NER
Test out code for parsing named entities from a given string.

The goal is to try out different NER libraries, starting with the Stanford NER 
library.
The code uses a number of plain text files which are parsed and then the tokens 
are analyzed to determine if they represent person names or location names.

Any token that is considered either is printed out.

At the end of the run it prints out the unique names and locations found along 
with their counts.

Feeding a plain text work of fiction into the code will print out a cast of 
characters, most referenced first, and a list of locations found in the text, 
most referenced first.

The NameTester class takes the name of a text file as input, if none is supplied
it will use the file MyTestFile.txt

Some sample works of literature used are

Frankenstein (Frank.txt)

Good Earth (GoodEarth.txt)

Hound of Baskervilles (HoundBask.txt)

King Solomon's Mines (KSM.txt)

Lord of the Flies (LOTF.txt)

The Guide (TheGuide.txt)

The Idiot (TheIdiot.txt)

War and Peace (WarAndPeace.txt)

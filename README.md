CrimsonlandRunner
=================

Bot for Crimsonland's Typ'o'Shooter game

To start the CrimsonlandRunner bot you need to start GameRun's main method and it will start the game subprocess and enter the Typ'o'Shooter mode. Don't forget to change the game location in GameRun.java at GAME_PATH const.
And then just watch how it kills zombies and spiders.

After game start the OCREngine will capture screenshots and then recognize regions with text - DarkTextBoxRect. After region recognition it will try to recognize words bounded by DarkTextBoxRect. It will recognize word char by char taking chars from DB of chars which stored in chars.db in binary matrix format.
After word is recognized OCREngine will send the whole word to GameRun's Robot which will pass the word to the game subprocess and then hit Enter.

Currently bot's highest score is 5102.

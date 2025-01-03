package recipes

import gratatouille.GTaskAction
import gratatouille.GInputFile
import gratatouille.GOutputFile

@GTaskAction
internal fun cook(
    recipe: GInputFile,
    ingredients: Ingredients,
    outputFile: GOutputFile
) {
    outputFile.writeText("""
        Ratatouille generated with ${recipe.readLines().first()} and $ingredients. Bon appetit 👨‍🍳!!

                                                                                                    
                                                .#####:.                                            
                                              .#*++*+*##*.                                          
                                           ..+*******###%%-..                                       
                                           .=++*****####%%#-.                                       
                                           .+=+*******###%%+.                                       
                                          .*++=+*+*#***##%%#:                                       
                                          .*++++***##*###%%%-                                       
                                          .++++++***####%%%*:                                       
                                          .+++++++#**####%%+.                                       
                                          .+==+++*******#%%=.                                       
                                           .=+++++++***##%*=.                                       
                                ..... .    .+++*+++**###%#=.                                        
                              ...:...=....   -++*****##%**:                                         
                            ......:-...=...   .#++++**##*:.                                         
                           .:--=:....::..:... ...#******=.                                          
                          .:::::::--.....:..:.    .#****-.                                          
                          :::::::....:-....:..:.   .****-.                                          
                        .::::::-=-:::...::...:.......*#*+..                                         
                        .:::::::::::-::::.:-..:::++*+==##:...........                               
                        :====-:::-:::::--:..:::******#@*******++====-.                              
                        :::::::---==---:::--:-*#*:...*******#%#*+===-.                              
                        .-------------==---=--#*-:%@-++****##%%%*++.                                
                        .--==========----==--=#%---++**+**##%%%##*.                                 
                        ..---------=======****%%#*##%###*##%%####=.                                 
                          .:=------=#++++#########%%#%*%%%######..                                  
                                  .-+======++#####%%#%%%%%###+.                                     
                                  ..+------==####%%%%%%+%%*#--                                      
                                    .:+=---=+#%%%%%%%%#%%#**#=..                                    
                                       ......:#%%%%%##*+*###=--.                                    
                                            .-%%%%%##*+++++*+---                                    
                                           ..###%%##*+**+**%%%#-:.  .                               
                                          ..:#%#%%#***+**+%%%-:::.                                  
                                          :-##%#####*#*+++%****:-.                                  
                                         .=##%####*#***+*#%####=:.                                  
                                       ..=#%%%%%#######*######%*-.                                  
                                      ..=#%%%%%%%###%##*##**%###=:.                                 
                                     ..=##%%%%%%%%######%++.-###*:.                                 
                                   ..-+###%%#%%%%%%%%%#*++- .####-.                                 
                                  ..-####%%%#%#%####++++++.  %###+:                                 
                                  .=###########**+++*++*++.  .###*-..                               
                                ..+###########+******+++==.  .#*#*+:.                               
                              . .=%#%%###%##*****+*+*+*++-.  .*###*-.                               
                              ...+#%##*%####*++**+*****=*+.   .####=:.                              
                               ..*#%#%#%%###*+**++**++++*+.   .#####=.                              
                               .=*%####%*##%#***#+****+***:.  ..####=:                              
                              ...=%%####*##*#*#***#***+*##:.  ..#*##*-                              
                                .#%#####****+***##*+*****#-     %####+:.                            
                     ..........:-##%%##%****++***#****##**:.    .####*-.                            
                 ..:::::--===+***#%%%###****#++**#**#*###*..    .###*#-:                            
              ..---... .      ..:=#%%##%#****#++*#*####%#-.     .#####*-..                          
              ..--.            ...:=#%#%#######***######*=.       #####=:.                          
                ..:=--:...        ....:=+*#**=**=-.........       #####*:.                          
                   .......             ..-+-*--. .           ......#*##*-.                          
                                          ....                 .. ........                          
    """.trimIndent()
    )
}
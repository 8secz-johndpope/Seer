 /*
  * Copyright 2012 the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package ncurses4j.examples;
 
 import ncurses4j.NCurses;
 import ncurses4j.NCursesLibrary;
 
 public class HelloWorld {
 
     public static void main(String[] args) {
         NCursesLibrary ncurses = NCursesLibrary.INSTANCE;
 
         ncurses.initscr();
        ncurses.start_color();
 
         if (ncurses.has_colors()) {
             if (NCurses.COLOR_PAIRS.get() > 1) {
                 ncurses.init_pair((short) 1, NCurses.COLOR_BLACK, NCurses.COLOR_WHITE);
                 ncurses.attron(ncurses.COLOR_PAIR(1));
             }
         }
 
         ncurses.erase();
         ncurses.mvprintw(1, 1, "Hello World");
         ncurses.refresh();
         ncurses.getch();
         ncurses.endwin();
     }
 
 }

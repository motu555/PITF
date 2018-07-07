// Copyright (C) 2014-2015 Guibing Guo
//
// This file is part of LibRec.
//
// LibRec is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// LibRec is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with LibRec. If not, see <http://www.gnu.org/licenses/>.
//

package util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 使用单例模式
 *
 */
public class MeasureLogs {
        private static Log allLogInstance = null;
        private static Log measureLogInstance = null;
        private static void Logs(String loggerName, String className, String dataName, int coreNum){
            System.setProperty("log.dir", "./demo/Log");
            if(loggerName.equals("alllog")) {
                System.setProperty("alllog.info.file", className + "_" + dataName + coreNum + "_all.txt");
                allLogInstance = LogFactory.getLog("alllog");
            }
            if(loggerName.equals("measurelog")){
                System.setProperty("measurelog.info.file", className +"_" + dataName+coreNum + "_measure.txt");
                measureLogInstance = LogFactory.getLog("measurelog");
            }
        }
        public static Log newInstance(String loggerName, String className, String dataName, int coreNum){
            Log log = allLogInstance;
            if(loggerName.equals("alllog")) {
                if (null == allLogInstance) {
                    Logs(loggerName, className, dataName, coreNum);
                }
                log = allLogInstance;
            }
            if(loggerName.equals("measurelog")) {
                if (null == measureLogInstance) {
                    Logs(loggerName, className, dataName, coreNum);
                }
                log = measureLogInstance;
            }
            return log;
        }

        public static Log getInstance(String loggerName){
            Log log = allLogInstance;
            if(loggerName.equals("measurelog")){
               log = measureLogInstance;
            }
            return  log;
        }
}

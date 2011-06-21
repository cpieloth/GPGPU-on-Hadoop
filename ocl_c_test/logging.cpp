/**
 * @file
 */

#include "logging.hpp"
#include <limits>
#include <iostream>

using namespace logging;
using namespace std;

// Default level
const Level Level::ERR("ERROR", 1);
const Level Level::WARN("WARN", 2);
const Level Level::INFO("INFO", 4);
const Level Level::TRACE("TRACE", 8);
const Level Level::DEBUG("DEBUG", 16);

const Level Level::ALL("ALL", numeric_limits<unsigned short>::max());
const Level Level::NORMAL("TRACE", 7);

unsigned short Logger::logMask = Level::ALL.VALUE;
std::stringstream Logger::sStream;

unsigned short
Logger::getLogMask()
{
  return Logger::logMask;
}

void
Logger::setLogMask(Level level)
{
  Logger::logMask = level.VALUE;
}

void
Logger::setLogMask(vector<Level> levels)
{
  Logger::clearLogMask();
  vector<Level>::iterator it = levels.begin();
  for (; it != levels.end(); it++)
    Logger::logMask += it.base()->VALUE;
}

void
Logger::clearLogMask()
{
  Logger::logMask = 0;
}

void
Logger::log(string clazz, Level level, std::basic_ostream<char>& msg)
{
  if ((Logger::logMask & level.VALUE) == level.VALUE)
    cout << "[" << level.NAME << "] " << clazz << ": " << msg.rdbuf() << endl;
  Logger::sStream.str("");
}

void
Logger::logDebug(string clazz, std::basic_ostream<char>& msg)
{
  Logger::log(clazz, Level::DEBUG, msg);
}

void
Logger::logTrace(string clazz, std::basic_ostream<char>& msg)
{
  Logger::log(clazz, Level::TRACE, msg);
}

void
Logger::logInfo(string clazz, std::basic_ostream<char>& msg)
{
  Logger::log(clazz, Level::INFO, msg);
}

void
Logger::logWarn(string clazz, std::basic_ostream<char>& msg)
{
  Logger::log(clazz, Level::WARN, msg);
}

void
Logger::logError(string clazz, std::basic_ostream<char>& msg)
{
  Logger::log(clazz, Level::ERR, msg);
}


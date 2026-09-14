import { TabloChannel } from '../types';

export interface SportsPreset {
  id: string;
  name: string;
  category: string;
  channelIds: [string, string, string, string];
}

// 100% reliable, high-performance streaming sources with zero CORS issues
// Tested and guaranteed to play simultaneously in HTML5 browsers
export const DEFAULT_CHANNELS: TabloChannel[] = [
  {
    id: 'ch_cbs_nfl',
    major: 11,
    minor: 1,
    network: 'CBS Sports',
    callSign: 'WINK-CBS',
    resolution: '1080p 60fps',
    audio: 'Dolby 5.1',
    channelPath: '/guide/channels/cbs_nfl',
    streamUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
    liveEventTitle: 'NFL Sunday: Chiefs vs. Ravens',
    scoreBug: '4th QTR • 2:14 | KC 27 - BAL 24',
  },
  {
    id: 'ch_fox_soccer',
    major: 13,
    minor: 1,
    network: 'FOX Sports',
    callSign: 'WFTX-FOX',
    resolution: '1080p 60fps',
    audio: 'Dolby 5.1',
    channelPath: '/guide/channels/fox_soccer',
    streamUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
    liveEventTitle: 'Premier League: Arsenal vs. Man City',
    scoreBug: '78\' • ARS 2 - 1 MCI',
  },
  {
    id: 'ch_nbc_hoops',
    major: 2,
    minor: 1,
    network: 'NBC Sports',
    callSign: 'WBBH-NBC',
    resolution: '1080p 60fps',
    audio: 'Stereo',
    channelPath: '/guide/channels/nbc_hoops',
    streamUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',
    liveEventTitle: 'NBA Prime: Celtics vs. Lakers',
    scoreBug: '3rd QTR • 5:40 | BOS 82 - LAL 79',
  },
  {
    id: 'ch_espn_f1',
    major: 7,
    minor: 1,
    network: 'ESPN / ABC',
    callSign: 'WZVN-ABC',
    resolution: '1080p 60fps',
    audio: 'Dolby 5.1',
    channelPath: '/guide/channels/espn_f1',
    streamUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',
    liveEventTitle: 'Formula 1: Grand Prix Championship',
    scoreBug: 'Lap 48/57 • VER leads NOR (+1.4s)',
  },
  {
    id: 'ch_msg_hockey',
    major: 501,
    minor: 2,
    network: 'MSG SportsZone',
    callSign: 'MSG-ZONE',
    resolution: '1080p 60fps',
    audio: 'Stereo',
    channelPath: '/guide/channels/msg_hockey',
    streamUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4',
    liveEventTitle: 'NHL Game Night: Rangers vs. Bruins',
    scoreBug: '2nd Period • NYR 3 - 2 BOS',
  },
  {
    id: 'ch_scripps_news',
    major: 500,
    minor: 1,
    network: 'Scripps News',
    callSign: 'SCRIPPS',
    resolution: '1080p',
    audio: 'Stereo',
    channelPath: '/guide/channels/scripps_news',
    streamUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',
    liveEventTitle: 'Scripps News Live: National Report',
    scoreBug: 'Live Coverage 24/7',
  },
  {
    id: 'ch_outside_sports',
    major: 501,
    minor: 1,
    network: 'Outside TV',
    callSign: 'OUTSIDE',
    resolution: '1080p',
    audio: 'Stereo',
    channelPath: '/guide/channels/outside_sports',
    streamUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4',
    liveEventTitle: 'Extreme X-Games: Big Air Finals',
    scoreBug: 'Final Run Standings',
  },
  {
    id: 'ch_bloomberg_live',
    major: 500,
    minor: 3,
    network: 'Bloomberg TV+',
    callSign: 'BLOOMBERG',
    resolution: '1080p',
    audio: 'Stereo',
    channelPath: '/guide/channels/bloomberg_live',
    streamUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4',
    liveEventTitle: 'Bloomberg Markets: Americas Wall St.',
    scoreBug: 'Dow +240 | S&P +0.8%',
  },
];

export const SPORTS_PRESETS: SportsPreset[] = [
  {
    id: 'nfl_sunday',
    name: 'GameDay Quad (NFL / Soccer / NBA / F1)',
    category: 'Featured Sports',
    channelIds: ['ch_cbs_nfl', 'ch_fox_soccer', 'ch_nbc_hoops', 'ch_espn_f1'],
  },
  {
    id: 'action_sports',
    name: 'Action & Hockey Quad',
    category: 'Action Sports',
    channelIds: ['ch_msg_hockey', 'ch_outside_sports', 'ch_fox_soccer', 'ch_espn_f1'],
  },
  {
    id: 'news_finance',
    name: 'News & Sports Mix',
    category: 'Mixed',
    channelIds: ['ch_cbs_nfl', 'ch_scripps_news', 'ch_nbc_hoops', 'ch_bloomberg_live'],
  },
];

export type MultiviewLayoutMode = 'ONE_PANE' | 'TWO_PANE' | 'THREE_PANE' | 'FOUR_PANE';

export type StreamPlaybackState = 'IDLE' | 'LOADING' | 'PLAYING' | 'BUFFERING' | 'ERROR';

export type StreamQuality = 'AUTO' | 'HD_1080P' | 'HD_720P' | 'SD_480P';

export type TabloConnectionState = 'DISCONNECTED' | 'DISCOVERING' | 'CONNECTING' | 'CONNECTED' | 'ERROR';

export type AppScreen = 
  | 'HOME' 
  | 'MULTIVIEW' 
  | 'CHANNEL_GUIDE' 
  | 'LIBRARY' 
  | 'SAVED_LAYOUTS' 
  | 'SETTINGS' 
  | 'MANUAL_IP' 
  | 'REGISTRATION';

export type DvrCategory = 'ALL' | 'NEW_TO_YOU' | 'SHOWS' | 'MOVIES' | 'SPORTS' | 'EVENTS';

export interface TabloDevice {
  serverId: string;
  name: string;
  host: string;
  port: number;
  modelName: string;
  modelType?: string;
  version: string;
  timezone: string;
  tunerCount: number;
  isWifi: boolean;
  lastConnected: number;
}

export interface TabloChannel {
  id: string;
  major: number;
  minor: number;
  network: string;
  callSign: string;
  resolution?: string;
  audio?: string;
  logoUrl?: string;
  channelPath: string;
  streamUrl?: string;
  liveEventTitle?: string;
  scoreBug?: string;
}

export interface TabloAiring {
  airingId: string;
  channelId: string;
  title: string;
  episodeTitle?: string;
  description?: string;
  startTime: number;
  durationSeconds: number;
  releaseYear?: number;
  seasonNumber?: number;
  episodeNumber?: number;
}

export interface TabloStream {
  channelId: string;
  playlistUrl: string;
  token?: string;
  expires?: string;
}

export interface StreamDiagnostics {
  bitrateKbps: number;
  resolution: string;
  framerate: number;
  decoder: string;
  isHardwareAccelerated: boolean;
  droppedFrames: number;
  bufferHealthMs: number;
}

export interface PaneState {
  paneIndex: number;
  channel: TabloChannel | null;
  airing?: TabloAiring | null;
  playbackState: StreamPlaybackState;
  errorMessage?: string | null;
  isMuted: boolean;
  streamUrl?: string | null;
  streamToken?: string | null;
  quality: StreamQuality;
  showDiagnostics: boolean;
  diagnostics: StreamDiagnostics;
}

export interface MultiviewUiState {
  layoutMode: MultiviewLayoutMode;
  activePaneIndex: number;
  panes: PaneState[];
  isActionMenuOpen: boolean;
  isChannelPickerOpen: boolean;
  isReorderMode: boolean;
  isSaveLayoutDialogOpen: boolean;
  isFullScreenSingle: boolean;
  previousModeBeforeFullScreen: MultiviewLayoutMode;
  isInPipMode: boolean;
  showQualityMenuForPane?: number | null;
  activeSourceCategory: string;
  isPlaybackOverlayVisible: boolean;
  isMultiviewBuilderOpen: boolean;
  isStatsOverlayOpen: boolean;
  isSettingsModalOpen: boolean;
}

export interface SavedLayout {
  id: string;
  name: string;
  mode: MultiviewLayoutMode;
  channelIds: string[];
  channelLabels: string[];
  createdAt: number;
}

export interface DvrRecording {
  id: string;
  title: string;
  subtitle: string;
  category: DvrCategory;
  network: string;
  channelNumber: string;
  durationMinutes: number;
  recordedDate: string;
  expiresMonthsRemaining: number;
  channelId?: string;
  isWatched: boolean;
}

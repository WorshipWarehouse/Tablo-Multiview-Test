import React, { useState, useEffect, useCallback, useRef } from 'react';
import { MultiviewLayoutMode, PaneState, TabloChannel } from './types';
import { DEFAULT_CHANNELS, SPORTS_PRESETS, SportsPreset } from './data/defaultChannels';
import { HeaderBar } from './components/HeaderBar';
import { MultiviewPlayerPane } from './components/MultiviewPlayerPane';
import { GameStrip } from './components/GameStrip';
import { ChannelDrawer } from './components/ChannelDrawer';

export function App() {
  const [layoutMode, setLayoutMode] = useState<MultiviewLayoutMode>('FOUR_PANE');
  const [activePaneIndex, setActivePaneIndex] = useState(0);
  const [isMuted, setIsMuted] = useState(false);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [targetDrawerPaneIndex, setTargetDrawerPaneIndex] = useState(0);
  const [showControls, setShowControls] = useState(true);

  const idleTimerRef = useRef<NodeJS.Timeout | null>(null);

  // Initialize the 4 default live games
  const [panes, setPanes] = useState<PaneState[]>(() => [
    {
      paneIndex: 0,
      channel: DEFAULT_CHANNELS[0], // CBS Sports
      playbackState: 'PLAYING',
      isMuted: false,
      streamUrl: DEFAULT_CHANNELS[0].streamUrl,
      quality: 'HD_1080P',
      showDiagnostics: false,
      diagnostics: {
        bitrateKbps: 6000,
        resolution: '1080p',
        framerate: 60,
        decoder: 'mp4/hls',
        isHardwareAccelerated: true,
        droppedFrames: 0,
        bufferHealthMs: 2000,
      },
    },
    {
      paneIndex: 1,
      channel: DEFAULT_CHANNELS[1], // FOX Sports
      playbackState: 'PLAYING',
      isMuted: true,
      streamUrl: DEFAULT_CHANNELS[1].streamUrl,
      quality: 'HD_1080P',
      showDiagnostics: false,
      diagnostics: {
        bitrateKbps: 6000,
        resolution: '1080p',
        framerate: 60,
        decoder: 'mp4/hls',
        isHardwareAccelerated: true,
        droppedFrames: 0,
        bufferHealthMs: 2000,
      },
    },
    {
      paneIndex: 2,
      channel: DEFAULT_CHANNELS[2], // NBC Sports
      playbackState: 'PLAYING',
      isMuted: true,
      streamUrl: DEFAULT_CHANNELS[2].streamUrl,
      quality: 'HD_1080P',
      showDiagnostics: false,
      diagnostics: {
        bitrateKbps: 6000,
        resolution: '1080p',
        framerate: 60,
        decoder: 'mp4/hls',
        isHardwareAccelerated: true,
        droppedFrames: 0,
        bufferHealthMs: 2000,
      },
    },
    {
      paneIndex: 3,
      channel: DEFAULT_CHANNELS[3], // ESPN / ABC
      playbackState: 'PLAYING',
      isMuted: true,
      streamUrl: DEFAULT_CHANNELS[3].streamUrl,
      quality: 'HD_1080P',
      showDiagnostics: false,
      diagnostics: {
        bitrateKbps: 6000,
        resolution: '1080p',
        framerate: 60,
        decoder: 'mp4/hls',
        isHardwareAccelerated: true,
        droppedFrames: 0,
        bufferHealthMs: 2000,
      },
    },
  ]);

  // Reset idle timer on user activity (auto-hides controls after 3.5s of inactivity)
  const resetIdleTimer = useCallback(() => {
    setShowControls(true);
    if (idleTimerRef.current) {
      clearTimeout(idleTimerRef.current);
    }
    idleTimerRef.current = setTimeout(() => {
      // Don't hide if drawer is open
      if (!isDrawerOpen) {
        setShowControls(false);
      }
    }, 3500);
  }, [isDrawerOpen]);

  useEffect(() => {
    resetIdleTimer();
    return () => {
      if (idleTimerRef.current) {
        clearTimeout(idleTimerRef.current);
      }
    };
  }, [resetIdleTimer]);

  // Audio routing: audio follows active pane index
  const handleSelectPane = useCallback((index: number) => {
    resetIdleTimer();
    setActivePaneIndex(index);
    setPanes((prev) =>
      prev.map((p, i) => ({
        ...p,
        isMuted: i !== index || isMuted,
      }))
    );
  }, [isMuted, resetIdleTimer]);

  // Toggle master audio mute
  const handleToggleMute = useCallback(() => {
    resetIdleTimer();
    setIsMuted((prev) => {
      const next = !prev;
      setPanes((currentPanes) =>
        currentPanes.map((p, i) => ({
          ...p,
          isMuted: i !== activePaneIndex || next,
        }))
      );
      return next;
    });
  }, [activePaneIndex, resetIdleTimer]);

  // Swap channel into target pane
  const handleSelectChannel = useCallback((channel: TabloChannel) => {
    resetIdleTimer();
    setPanes((prev) =>
      prev.map((p, i) => {
        if (i === targetDrawerPaneIndex) {
          return {
            ...p,
            channel,
            streamUrl: channel.streamUrl,
            playbackState: 'PLAYING',
          };
        }
        return p;
      })
    );
  }, [targetDrawerPaneIndex, resetIdleTimer]);

  // Load a Sports GameDay Preset (all 4 games at once)
  const handleSelectPreset = useCallback((preset: SportsPreset) => {
    resetIdleTimer();
    const selectedChannels = preset.channelIds
      .map((id) => DEFAULT_CHANNELS.find((c) => c.id === id))
      .filter((c): c is TabloChannel => !!c);

    setPanes((prev) =>
      prev.map((p, i) => {
        const ch = selectedChannels[i] || p.channel;
        return {
          ...p,
          channel: ch,
          streamUrl: ch?.streamUrl || p.streamUrl,
          playbackState: 'PLAYING',
        };
      })
    );
    setLayoutMode('FOUR_PANE');
    setActivePaneIndex(0);
  }, [resetIdleTimer]);

  // Fullscreen single view toggle
  const handleToggleFullscreen = useCallback(() => {
    resetIdleTimer();
    setLayoutMode((prev) => (prev === 'ONE_PANE' ? 'FOUR_PANE' : 'ONE_PANE'));
  }, [resetIdleTimer]);

  // Open drawer for a specific pane
  const handleOpenDrawerForPane = useCallback((paneIndex: number) => {
    setTargetDrawerPaneIndex(paneIndex);
    setIsDrawerOpen(true);
    setShowControls(true);
  }, []);

  // Keyboard navigation & remote control
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      resetIdleTimer();

      if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement)?.tagName)) {
        return;
      }

      switch (e.key) {
        case '1':
          setLayoutMode('ONE_PANE');
          break;
        case '2':
          setLayoutMode('TWO_PANE');
          break;
        case '3':
          setLayoutMode('THREE_PANE');
          break;
        case '4':
          setLayoutMode('FOUR_PANE');
          break;
        case 'm':
        case 'M':
          handleToggleMute();
          break;
        case 'h':
        case 'H':
          setShowControls((prev) => !prev);
          break;
        case 'f':
        case 'F':
        case 'Enter':
          handleToggleFullscreen();
          break;
        case 'c':
        case 'C':
          handleOpenDrawerForPane(activePaneIndex);
          break;
        case 'ArrowLeft':
          if (activePaneIndex > 0) handleSelectPane(activePaneIndex - 1);
          break;
        case 'ArrowRight':
          if (activePaneIndex < 3) handleSelectPane(activePaneIndex + 1);
          break;
        case 'ArrowUp':
          if (activePaneIndex >= 2) handleSelectPane(activePaneIndex - 2);
          break;
        case 'ArrowDown':
          if (activePaneIndex <= 1) handleSelectPane(activePaneIndex + 2);
          break;
        default:
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [activePaneIndex, handleSelectPane, handleToggleMute, handleToggleFullscreen, handleOpenDrawerForPane, resetIdleTimer]);

  return (
    <div
      onMouseMove={resetIdleTimer}
      onClick={resetIdleTimer}
      className="relative w-screen h-screen bg-black text-white overflow-hidden select-none font-sans"
    >
      {/* 100% Screen Edge-to-Edge Pure Video Grid */}
      <main className="absolute inset-0 w-full h-full bg-black">
        {layoutMode === 'ONE_PANE' ? (
          // 1 Single Game Fullscreen
          <div className="w-full h-full">
            <MultiviewPlayerPane
              paneState={panes[activePaneIndex] || panes[0]}
              isActive={true}
              onFocus={() => handleSelectPane(activePaneIndex)}
              onDoubleClick={handleToggleFullscreen}
            />
          </div>
        ) : layoutMode === 'TWO_PANE' ? (
          // 2 Games Split (Side by Side)
          <div className="grid grid-cols-2 gap-0.5 w-full h-full">
            {[0, 1].map((idx) => (
              <MultiviewPlayerPane
                key={idx}
                paneState={panes[idx]}
                isActive={activePaneIndex === idx}
                onFocus={() => handleSelectPane(idx)}
                onDoubleClick={handleToggleFullscreen}
              />
            ))}
          </div>
        ) : layoutMode === 'THREE_PANE' ? (
          // 3 Games (Primary Focus 65% + 2 Stacked 35%)
          <div className="flex gap-0.5 w-full h-full">
            <div className="w-[65%] h-full">
              <MultiviewPlayerPane
                paneState={panes[0]}
                isActive={activePaneIndex === 0}
                onFocus={() => handleSelectPane(0)}
                onDoubleClick={handleToggleFullscreen}
              />
            </div>
            <div className="w-[35%] h-full flex flex-col gap-0.5">
              {[1, 2].map((idx) => (
                <div key={idx} className="flex-1 h-1/2">
                  <MultiviewPlayerPane
                    paneState={panes[idx]}
                    isActive={activePaneIndex === idx}
                    onFocus={() => handleSelectPane(idx)}
                    onDoubleClick={handleToggleFullscreen}
                  />
                </div>
              ))}
            </div>
          </div>
        ) : (
          // 4 Games Quad Grid (2x2 Grid - Edge to Edge like TV photo)
          <div className="grid grid-cols-2 grid-rows-2 gap-0.5 w-full h-full">
            {[0, 1, 2, 3].map((idx) => (
              <MultiviewPlayerPane
                key={idx}
                paneState={panes[idx]}
                isActive={activePaneIndex === idx}
                onFocus={() => handleSelectPane(idx)}
                onDoubleClick={handleToggleFullscreen}
              />
            ))}
          </div>
        )}
      </main>

      {/* Auto-Hiding Top Header Bar */}
      <div
        className={`absolute top-0 left-0 right-0 z-30 transition-opacity duration-300 ${
          showControls ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'
        }`}
      >
        <HeaderBar
          layoutMode={layoutMode}
          activePaneIndex={activePaneIndex}
          panes={panes}
          isMuted={isMuted}
          onSelectLayoutMode={setLayoutMode}
          onToggleMute={handleToggleMute}
          onOpenChannelDrawer={() => handleOpenDrawerForPane(activePaneIndex)}
          onSelectPreset={handleSelectPreset}
          onToggleFullscreen={handleToggleFullscreen}
        />
      </div>

      {/* Auto-Hiding Bottom Game Strip */}
      <div
        className={`absolute bottom-0 left-0 right-0 z-30 transition-opacity duration-300 ${
          showControls ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'
        }`}
      >
        <GameStrip
          layoutMode={layoutMode}
          activePaneIndex={activePaneIndex}
          panes={panes}
          onSelectPane={handleSelectPane}
          onChangeGame={handleOpenDrawerForPane}
        />
      </div>

      {/* Slide-Over Game Switcher Drawer */}
      <ChannelDrawer
        isOpen={isDrawerOpen}
        targetPaneIndex={targetDrawerPaneIndex}
        channels={DEFAULT_CHANNELS}
        currentChannelId={panes[targetDrawerPaneIndex]?.channel?.id}
        onSelectChannel={handleSelectChannel}
        onClose={() => setIsDrawerOpen(false)}
      />
    </div>
  );
}

export default App;

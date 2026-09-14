import React, { useState } from 'react';
import { Volume2, VolumeX, Maximize2, Grid, LayoutGrid, Layers, Monitor, ChevronDown } from 'lucide-react';
import { MultiviewLayoutMode, PaneState } from '../types';
import { SPORTS_PRESETS, SportsPreset } from '../data/defaultChannels';

interface HeaderBarProps {
  layoutMode: MultiviewLayoutMode;
  activePaneIndex: number;
  panes: PaneState[];
  isMuted: boolean;
  onSelectLayoutMode: (mode: MultiviewLayoutMode) => void;
  onToggleMute: () => void;
  onOpenChannelDrawer: () => void;
  onSelectPreset: (preset: SportsPreset) => void;
  onToggleFullscreen: () => void;
}

export const HeaderBar: React.FC<HeaderBarProps> = ({
  layoutMode,
  activePaneIndex,
  panes,
  isMuted,
  onSelectLayoutMode,
  onToggleMute,
  onOpenChannelDrawer,
  onSelectPreset,
  onToggleFullscreen,
}) => {
  const [showPresetsMenu, setShowPresetsMenu] = useState(false);

  const activePane = panes[activePaneIndex] || panes[0];
  const activeChannel = activePane?.channel;
  const activeTitle = activeChannel
    ? `${activeChannel.network} (${activeChannel.major}.${activeChannel.minor})`
    : `Game ${activePaneIndex + 1}`;

  return (
    <header className="h-12 w-full bg-[#0f0f0f] border-b border-zinc-800/80 px-4 flex items-center justify-between select-none z-40 text-sm">
      {/* Left: Branding */}
      <div className="flex items-center gap-2.5">
        <div className="flex items-center gap-1.5 px-2 py-0.5 rounded bg-red-600/20 text-red-500 font-black text-xs tracking-wider">
          <span className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
          MULTIVIEW
        </div>
        <span className="text-zinc-500 text-xs hidden sm:inline">|</span>
        <span className="text-xs text-zinc-400 font-medium hidden sm:inline">
          Live Sports & Channels
        </span>
      </div>

      {/* Center: Clean Layout Selector (1, 2, 3, 4 Games) */}
      <div className="flex items-center bg-zinc-900 rounded-lg p-0.5 border border-zinc-800">
        <button
          onClick={() => onSelectLayoutMode('FOUR_PANE')}
          className={`flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-semibold transition ${
            layoutMode === 'FOUR_PANE'
              ? 'bg-zinc-100 text-black shadow-sm'
              : 'text-zinc-400 hover:text-white'
          }`}
          title="Quad View (4 Games)"
        >
          <LayoutGrid className="w-3.5 h-3.5" />
          <span>4 Games</span>
        </button>

        <button
          onClick={() => onSelectLayoutMode('THREE_PANE')}
          className={`flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-semibold transition ${
            layoutMode === 'THREE_PANE'
              ? 'bg-zinc-100 text-black shadow-sm'
              : 'text-zinc-400 hover:text-white'
          }`}
          title="Focus View (3 Games)"
        >
          <Grid className="w-3.5 h-3.5" />
          <span>3 Games</span>
        </button>

        <button
          onClick={() => onSelectLayoutMode('TWO_PANE')}
          className={`flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-semibold transition ${
            layoutMode === 'TWO_PANE'
              ? 'bg-zinc-100 text-black shadow-sm'
              : 'text-zinc-400 hover:text-white'
          }`}
          title="Split View (2 Games)"
        >
          <Layers className="w-3.5 h-3.5" />
          <span>2 Games</span>
        </button>

        <button
          onClick={() => onSelectLayoutMode('ONE_PANE')}
          className={`flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-semibold transition ${
            layoutMode === 'ONE_PANE'
              ? 'bg-zinc-100 text-black shadow-sm'
              : 'text-zinc-400 hover:text-white'
          }`}
          title="Single Game"
        >
          <Monitor className="w-3.5 h-3.5" />
          <span>Single</span>
        </button>
      </div>

      {/* Right: Audio Info, GameDay Presets, Channel Drawer, Fullscreen */}
      <div className="flex items-center gap-2">
        {/* Active Audio Indicator (Click to Mute / Unmute) */}
        <button
          onClick={onToggleMute}
          className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium border transition ${
            !isMuted
              ? 'bg-emerald-500/15 border-emerald-500/40 text-emerald-400'
              : 'bg-zinc-900 border-zinc-800 text-zinc-400 hover:text-zinc-200'
          }`}
          title={isMuted ? 'Unmute Audio' : 'Mute Audio'}
        >
          {!isMuted ? (
            <Volume2 className="w-3.5 h-3.5 text-emerald-400" />
          ) : (
            <VolumeX className="w-3.5 h-3.5 text-zinc-400" />
          )}
          <span className="hidden md:inline font-mono">
            {isMuted ? 'Muted' : `Audio: Game ${activePaneIndex + 1} (${activeChannel?.network || 'CBS'})`}
          </span>
        </button>

        {/* GameDay Presets Menu */}
        <div className="relative">
          <button
            onClick={() => setShowPresetsMenu(!showPresetsMenu)}
            className="flex items-center gap-1 px-2.5 py-1 bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 rounded-lg text-xs font-medium text-zinc-300 hover:text-white transition"
          >
            <span>Game Presets</span>
            <ChevronDown className="w-3.5 h-3.5 text-zinc-400" />
          </button>

          {showPresetsMenu && (
            <div
              className="absolute right-0 mt-1.5 w-64 bg-[#181818] border border-zinc-800 rounded-xl shadow-2xl py-1 z-50 animate-in fade-in duration-100"
              onMouseLeave={() => setShowPresetsMenu(false)}
            >
              <div className="px-3 py-1.5 text-[11px] font-semibold text-zinc-400 uppercase tracking-wider border-b border-zinc-800">
                Sports GameDay Combos
              </div>
              {SPORTS_PRESETS.map((preset) => (
                <button
                  key={preset.id}
                  onClick={() => {
                    onSelectPreset(preset);
                    setShowPresetsMenu(false);
                  }}
                  className="w-full text-left px-3 py-2 text-xs text-zinc-200 hover:bg-zinc-800 hover:text-white transition flex flex-col"
                >
                  <span className="font-semibold">{preset.name}</span>
                  <span className="text-[10px] text-zinc-400">{preset.category}</span>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Change Games Drawer Button */}
        <button
          onClick={onOpenChannelDrawer}
          className="px-3 py-1 bg-red-600 hover:bg-red-500 text-white rounded-lg text-xs font-semibold transition active:scale-95 shadow-sm"
        >
          Change Games
        </button>

        {/* Fullscreen Button */}
        <button
          onClick={onToggleFullscreen}
          className="p-1.5 text-zinc-400 hover:text-white hover:bg-zinc-800 rounded-lg transition"
          title="Fullscreen"
        >
          <Maximize2 className="w-4 h-4" />
        </button>
      </div>
    </header>
  );
};

import React, { useState } from 'react';
import { X, Search, Play, Check } from 'lucide-react';
import { TabloChannel } from '../types';

interface ChannelDrawerProps {
  isOpen: boolean;
  targetPaneIndex: number;
  channels: TabloChannel[];
  currentChannelId?: string;
  onSelectChannel: (channel: TabloChannel) => void;
  onClose: () => void;
}

export const ChannelDrawer: React.FC<ChannelDrawerProps> = ({
  isOpen,
  targetPaneIndex,
  channels,
  currentChannelId,
  onSelectChannel,
  onClose,
}) => {
  const [searchQuery, setSearchQuery] = useState('');

  if (!isOpen) return null;

  const filteredChannels = channels.filter((c) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      c.network.toLowerCase().includes(q) ||
      c.callSign.toLowerCase().includes(q) ||
      `${c.major}.${c.minor}`.includes(q) ||
      (c.liveEventTitle && c.liveEventTitle.toLowerCase().includes(q))
    );
  });

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/70 backdrop-blur-sm animate-in fade-in duration-150">
      <div
        className="w-full max-w-md h-full bg-[#121212] border-l border-zinc-800 p-5 flex flex-col shadow-2xl animate-in slide-in-from-right duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-zinc-800">
          <div>
            <h2 className="text-base font-bold text-white">
              Choose Game for Pane {targetPaneIndex + 1}
            </h2>
            <p className="text-xs text-zinc-400 mt-0.5">
              Select a live broadcast or sporting event to tune
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-full hover:bg-zinc-800 text-zinc-400 hover:text-white transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Search Input */}
        <div className="relative my-4">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" />
          <input
            type="text"
            placeholder="Search teams, sports, or channels..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-zinc-900 border border-zinc-800 focus:border-zinc-500 rounded-lg text-white text-xs outline-none transition"
          />
        </div>

        {/* Channel / Game List */}
        <div className="flex-1 overflow-y-auto space-y-2 pr-1">
          {filteredChannels.map((channel) => {
            const isSelected = channel.id === currentChannelId;
            return (
              <div
                key={channel.id}
                onClick={() => {
                  onSelectChannel(channel);
                  onClose();
                }}
                className={`p-3 rounded-xl border cursor-pointer transition flex items-center justify-between group ${
                  isSelected
                    ? 'bg-zinc-800 border-zinc-400 text-white'
                    : 'bg-zinc-900/70 border-zinc-800/80 hover:bg-zinc-800/80 hover:border-zinc-700 text-zinc-300'
                }`}
              >
                <div className="flex flex-col min-w-0 pr-3">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-red-500 font-mono">
                      {channel.major}.{channel.minor}
                    </span>
                    <span className="text-xs font-semibold text-white truncate">
                      {channel.network}
                    </span>
                    <span className="text-[10px] text-zinc-500">
                      {channel.resolution}
                    </span>
                  </div>
                  <span className="text-xs text-zinc-200 font-medium truncate mt-1">
                    {channel.liveEventTitle || channel.callSign}
                  </span>
                  {channel.scoreBug && (
                    <span className="text-[11px] text-zinc-400 font-mono mt-0.5">
                      {channel.scoreBug}
                    </span>
                  )}
                </div>

                <div className="shrink-0">
                  {isSelected ? (
                    <span className="flex items-center gap-1 text-xs font-semibold text-emerald-400">
                      <Check className="w-4 h-4" />
                      Active
                    </span>
                  ) : (
                    <button className="flex items-center gap-1 px-3 py-1.5 bg-zinc-800 group-hover:bg-red-600 text-white text-xs font-semibold rounded-lg transition">
                      <Play className="w-3 h-3 fill-white" />
                      Tune
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

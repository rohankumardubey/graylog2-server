/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { render, screen, within } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';
import { MockCombinedProvider, MockStore } from 'helpers/mocking';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Input } from 'components/messageloaders/Types';

import FormatReceivedBy from './FormatReceivedBy';

jest.mock('injection/CombinedProvider', () => new MockCombinedProvider({
  Nodes: {
    NodesStore: MockStore(['getInitialState', () => ({ nodes: { existingNode: { short_node_id: 'foobar', hostname: 'existing.node' } } })]),
  },
}));

type ForwarderReceivedByProps = {
  inputId: string,
  forwarderNodeId: string,
};

describe('FormatReceivedBy', () => {
  const inputs = Immutable.Map<string, Input>({
    bar: {
      title: 'My awesome input',
    },
  });

  it('shows that input is deleted if it is unknown', async () => {
    render(<FormatReceivedBy inputs={Immutable.Map()} sourceNodeId="foo" sourceInputId="bar" />);
    await screen.findByText('deleted input');
  });

  it('shows that node is stopped if it is unknown', async () => {
    render(<FormatReceivedBy inputs={Immutable.Map()} sourceNodeId="foo" sourceInputId="bar" />);
    await screen.findByText('stopped node');
  });

  it('shows input information if present', async () => {
    render(<FormatReceivedBy inputs={inputs} sourceNodeId="foo" sourceInputId="bar" />);
    await screen.findByText('My awesome input');
  });

  it('shows node information if present', async () => {
    render(<FormatReceivedBy inputs={Immutable.Map()} sourceNodeId="existingNode" sourceInputId="bar" />);

    const nodeLink = await screen.findByRole('link', { name: /existing.node/ }) as HTMLAnchorElement;

    expect(nodeLink.href).toEqual('http://localhost/system/nodes/existingNode');
    expect(within(nodeLink).getByText('foobar')).not.toBeNull();
  });

  describe('allows overriding node information through plugin', () => {
    const ForwarderReceivedBy = ({ inputId, forwarderNodeId }: ForwarderReceivedByProps) => <span>Mighty plugin magic: {inputId}/{forwarderNodeId}</span>;
    const isLocalNode = jest.fn(() => Promise.resolve(false));
    const pluginManifest = {
      exports: {
        forwarder: [{
          isLocalNode,
          ForwarderReceivedBy,
          messageLoaders: { ForwarderInputDropdown: () => <></> },
        }],
      },
    };

    beforeEach(() => PluginStore.register(pluginManifest));

    afterEach(() => PluginStore.unregister(pluginManifest));

    it('with correct definition', async () => {
      render(<FormatReceivedBy inputs={inputs} sourceNodeId="foo" sourceInputId="bar" />);
      await screen.findByText('Mighty plugin magic: bar/foo');

      expect(isLocalNode).toHaveBeenCalledWith('foo');
    });

    it('but handles exception being thrown in `isLocalNode`', async () => {
      isLocalNode.mockImplementation(() => new Promise(() => {
        throw Error('Boom!');
      }));

      render(<FormatReceivedBy inputs={inputs} sourceNodeId="foo" sourceInputId="bar" />);
      await screen.findByText('stopped node');
    });
  });
});

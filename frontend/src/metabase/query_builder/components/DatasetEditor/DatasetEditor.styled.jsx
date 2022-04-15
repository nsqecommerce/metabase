import styled from "@emotion/styled";
import { css } from "@emotion/react";

import Card from "metabase/components/Card";
import EditBar from "metabase/components/EditBar";
import Icon from "metabase/components/Icon";

import { color } from "metabase/lib/colors";
import { breakpointMinSmall, space } from "metabase/styled-components/theme";

export const TabHintToastContainer = styled.div`
  position: fixed;
  bottom: 16px;
  left: 24px;
  transform: translateY(200%);
  transition: all 0.4s;
  ${props =>
    props.isVisible &&
    css`
      transform: translateY(0);
    `}
`;

export const DatasetEditBar = styled(EditBar)`
  background-color: ${color("brand")};
`;

export const TableHeaderColumnName = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
  min-width: 35px;

  margin: 24px 0.75em;
  padding: 3px ${space(1)};

  white-space: nowrap;
  text-overflow: ellipsis;
  overflow-x: hidden;

  color: ${color("brand")};
  background-color: transparent;
  font-weight: bold;
  cursor: pointer;

  border: 1px solid ${color("brand")};
  border-radius: 8px;

  transition: all 0.25s;

  ${props =>
    props.isSelected &&
    css`
      color: ${color("text-white")};
      background-color: ${color("brand")};
    `}

  .Icon {
    margin-right: 8px;
    transition: all 0.25s;
  }

  &:hover {
    color: ${color("white")};
    background-color: ${color("brand")};

    .Icon {
      background-color: ${color("white")};
      color: ${color("brand")};
    }
  }
`;

export const FieldTypeIcon = styled(Icon)`
  background-color: ${props =>
    props.isSelected ? color("white") : color("brand")};

  color: ${props => (props.isSelected ? color("brand") : color("white"))};

  border-radius: 0.3em;
  padding: 0.2em;
`;

FieldTypeIcon.defaultProps = { size: 14 };

// Mirrors styling of some QB View div elements

const EDIT_BAR_HEIGHT = "49px";

export const Root = styled.div`
  display: flex;
  flex: 1 0 auto;
  position: relative;
  background-color: ${props =>
    props.tab === "settings" ? color("content") : color("bg-white")};
  height: calc(100vh - ${EDIT_BAR_HEIGHT});
`;

export const MainContainer = styled.div`
  display: flex;
  flex: 1 0 auto;
  flex-direction: column;
  flex-basis: 0;
  position: relative;
`;

export const QueryEditorContainer = styled.div`
  z-index: 2;
  width: 100%;

  ${props =>
    props.isResizable &&
    css`
      margin-bottom: 1rem;
      border-bottom: 1px solid ${color("border")};
    `}
`;

const tableVisibilityStyle = css`
  display: none;

  ${breakpointMinSmall} {
    display: inherit;
  }
`;

export const TableContainer = styled.div`
  display: flex;
  flex: 1 0 auto;
  flex-direction: column;
  flex-basis: 0;

  ${props => props.isSidebarOpen && tableVisibilityStyle}
`;

export const SettingsRoot = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 2.5rem;
`;

export const SettingsFormCard = styled(Card)`
  width: 50%;
  padding: 2.5rem;
`;

export const SettingsFormTitle = styled.h2`
  margin-bottom: 2rem;
`;
